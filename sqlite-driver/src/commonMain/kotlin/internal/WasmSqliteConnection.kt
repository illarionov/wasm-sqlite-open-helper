/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.internal

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic
import ru.pixnews.wasm.sqlite.driver.dsl.OpenFlags
import ru.pixnews.wasm.sqlite.driver.dsl.OpenParamsBlock
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.internal.wasmSqliteCleaner
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3CApi
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3Result

internal class WasmSqliteConnection(
    private val databaseLabel: String,
    private val openParams: OpenParamsBlock,
    connectionPtr: WasmPtr<SqliteDb>,
    private val cApi: Sqlite3CApi,
    rootLogger: Logger,
) : SQLiteConnection {
    private val logger = rootLogger.withTag("WasmSqliteConnection")
    private val connectionPtrResource = ConnectionPtrClosable(connectionPtr)
    private val connectionPtrResourceCleanable = wasmSqliteCleaner.register(
        obj = this,
        cleanAction = ConnectionPtrClosableCleanAction(databaseLabel, connectionPtrResource, cApi, logger),
    )

    fun configure() {
        checkCurrentThread(logger, connectionPtrResource.threadId)
        // Register the localized collators.
        if (openParams.openFlags.contains(OpenFlags.LOCALIZED_COLLATORS)) {
            val newLocale: String = openParams.locale.icuId
            val errCode = cApi.db.registerLocalizedCollators(connectionPtrResource.nativePtr, newLocale)
            if (errCode is Sqlite3Result.Error) {
                // This can happen if sub-objects aren't closed first.  Make sure the caller knows.
                logger.i { "register_localized_collators(${connectionPtrResource.nativePtr}) failed: ${errCode.info}" }
                throwSqliteException(errCode.info, "Could not setup localized collators.")
            }
        }
    }

    override fun prepare(sql: String): SQLiteStatement {
        if (connectionPtrResource.isClosed.value) {
            throwSqliteException("Connection closed", SqliteResultCode.SQLITE_MISUSE)
        }
        checkCurrentThread(logger, connectionPtrResource.threadId)

        return when (val statementPtr = cApi.statement.sqlite3PrepareV2(connectionPtrResource.nativePtr, sql)) {
            is Sqlite3Result.Success -> {
                logger.v { "Prepared statement ${statementPtr.value} on connection ${connectionPtrResource.nativePtr}" }

                WasmSqliteStatement(
                    databaseLabel = databaseLabel,
                    connectionPtr = connectionPtrResource,
                    statementPtr = statementPtr.value,
                    cApi = cApi,
                    rootLogger = logger,
                )
            }

            is Sqlite3Result.Error -> cApi.readErrorThrowSqliteException(
                connectionPtrResource.nativePtr,
                ", while compiling: $sql",
            )
        }
    }

    override fun close() {
        logger.v { "close($connectionPtrResource) " }
        val alreadyClosed = connectionPtrResource.isClosed.getAndSet(true)
        if (!alreadyClosed) {
            closeConnection(databaseLabel, connectionPtrResource, cApi, logger)
        }
        connectionPtrResourceCleanable.clean()
    }

    internal class ConnectionPtrClosable(
        val nativePtr: WasmPtr<SqliteDb>,
        val isClosed: AtomicBoolean = atomic(false),
        val threadId: ULong = currentThreadId,
    )

    private class ConnectionPtrClosableCleanAction(
        val databaseLabel: String,
        val ptr: ConnectionPtrClosable,
        val cApi: Sqlite3CApi,
        val rootLogger: Logger,
    ) : () -> Unit {
        override fun invoke() {
            val alreadyClosed = ptr.isClosed.getAndSet(true)
            if (!alreadyClosed) {
                onConnectionLeaked()
                closeConnection(databaseLabel, ptr, cApi, rootLogger)
            }
        }

        private fun onConnectionLeaked() {
            rootLogger.e {
                "A SQLiteConnection object for database '" +
                        databaseLabel + "' was leaked!  Please fix your application " +
                        "to end transactions in progress properly and to close the database " +
                        "when it is no longer needed."
            }
        }
    }

    private companion object {
        fun closeConnection(
            databaseLabel: String,
            closableConnectionPtr: ConnectionPtrClosable,
            cApi: Sqlite3CApi,
            logger: Logger,
        ) {
            if (closableConnectionPtr.isClosed.getAndSet(true)) {
                return
            }
            logger.v { "closeConnection($closableConnectionPtr) " }
            checkCurrentThread(logger, closableConnectionPtr.threadId)

            val rawConnectionPtr = closableConnectionPtr.nativePtr
            cApi.db.sqlite3Close(rawConnectionPtr)
                // This can happen if sub-objects aren't closed first.  Make sure the caller knows.
                .throwOnError("Could not close database `$databaseLabel`")
        }
    }
}
