/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.internal

import androidx.sqlite.SQLiteStatement
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic
import ru.pixnews.wasm.sqlite.driver.internal.WasmSqliteConnection.ConnectionPtrClosable
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.exception.throwAndroidSqliteException
import ru.pixnews.wasm.sqlite.open.helper.internal.wasmSqliteCleaner
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteStatement
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3CApi
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3Result
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3StatementFunctions

internal class WasmSqliteStatement(
    private val databaseLabel: String,
    connectionPtr: ConnectionPtrClosable,
    statementPtr: WasmPtr<SqliteStatement>,
    private val cApi: Sqlite3CApi,
    rootLogger: Logger,
) : SQLiteStatement {
    private val logger = rootLogger.withTag("WasmSqliteStatement")
    private val statementApi = cApi.statement
    private val statementPtrResource = StatementPtrClosable(connectionPtr, statementPtr)
    private val connectionPtrResourceCleanable = wasmSqliteCleaner.register(
        obj = this,
        cleanAction = StatementPtrClosableCleanAction(databaseLabel, statementPtrResource, cApi, logger),
    )
    private val rawStatementPtr: WasmPtr<SqliteStatement> get() = statementPtrResource.statementPtr
    private val rawConnectionPtr: WasmPtr<SqliteDb> get() = statementPtrResource.connectionPtr.nativePtr

    override fun bindBlob(index: Int, value: ByteArray) = executeSqliteCApiOrThrow { api, _, statement ->
        api.sqlite3BindBlobTransient(statement, index, value)
    }

    override fun bindDouble(index: Int, value: Double) = executeSqliteCApiOrThrow { statementApi, _, statementPtr ->
        statementApi.sqlite3BindDouble(statementPtr, index, value)
    }

    override fun bindLong(index: Int, value: Long) = executeSqliteCApiOrThrow { statementApi, _, statementPtr ->
        statementApi.sqlite3BindLong(statementPtr, index, value)
    }

    override fun bindNull(index: Int) = executeSqliteCApiOrThrow { statementApi, _, statementPtr ->
        statementApi.sqlite3BindNull(statementPtr, index)
    }

    override fun bindText(index: Int, value: String) = executeSqliteCApiOrThrow { statementApi, _, statementPtr ->
        statementApi.sqlite3BindStringTransient(statementPtr, index, value)
    }

    override fun clearBindings() = executeSqliteCApiOrThrow { statementApi, _, statementPtr ->
        statementApi.sqlite3ClearBindings(statementPtr)
    }

    override fun close() {
        finalizeStatement(databaseLabel, statementPtrResource, cApi, logger)
        connectionPtrResourceCleanable.clean()
    }

    override fun getBlob(index: Int): ByteArray {
        throwIfFinalized()
        throwIfNoRow()
        throwIfInvalidColumn(index)
        return statementApi.sqlite3ColumnBlob(rawStatementPtr, index)
    }

    override fun getColumnCount(): Int {
        throwIfFinalized()
        return statementApi.sqlite3ColumnCount(rawStatementPtr)
    }

    override fun getColumnName(index: Int): String {
        throwIfFinalized()
        throwIfInvalidColumn(index)
        return statementApi.sqlite3ColumnName(rawStatementPtr, index) ?: throwAndroidSqliteException(
            "Can not get column name",
            SqliteResultCode.SQLITE_NOMEM,
        )
    }

    override fun getDouble(index: Int): Double {
        throwIfFinalized()
        throwIfNoRow()
        throwIfInvalidColumn(index)
        return statementApi.sqlite3ColumnDouble(rawStatementPtr, index)
    }

    override fun getLong(index: Int): Long {
        throwIfFinalized()
        throwIfNoRow()
        throwIfInvalidColumn(index)
        return statementApi.sqlite3ColumnInt64(rawStatementPtr, index)
    }

    override fun getText(index: Int): String {
        throwIfFinalized()
        throwIfNoRow()
        throwIfInvalidColumn(index)
        return statementApi.sqlite3ColumnText(rawStatementPtr, index) ?: throwAndroidSqliteException(
            "Can not get text column",
            SqliteResultCode.SQLITE_NOMEM,
        )
    }

    override fun isNull(index: Int): Boolean {
        throwIfFinalized()
        throwIfNoRow()
        throwIfInvalidColumn(index)
        return statementApi.sqlite3ColumnType(rawStatementPtr, index) == SqliteColumnType.SQLITE_NULL
    }

    override fun reset() = executeSqliteCApiOrThrow { statementApi, _, statementPtr ->
        statementApi.sqlite3Reset(statementPtr)
    }

    override fun step(): Boolean {
        throwIfFinalized()
        val error = statementApi.sqlite3Step(rawStatementPtr)
        return when (error) {
            SqliteResultCode.SQLITE_ROW -> true
            SqliteResultCode.SQLITE_DONE -> true
            else -> cApi.readErrorThrowAndroidSqliteException(rawConnectionPtr)
        }
    }

    private inline fun executeSqliteCApiOrThrow(
        block: (Sqlite3StatementFunctions, WasmPtr<SqliteDb>, WasmPtr<SqliteStatement>) -> SqliteResultCode,
    ) {
        throwIfFinalized()
        val err = block(statementApi, rawConnectionPtr, rawStatementPtr)
        if (err != SqliteResultCode.SQLITE_OK) {
            cApi.readErrorThrowAndroidSqliteException(rawConnectionPtr)
        }
    }

    private fun throwIfFinalized() {
        if (statementPtrResource.isFinalized.value) {
            throwAndroidSqliteException("Statement closed", SqliteResultCode.SQLITE_MISUSE)
        }
    }

    private fun throwIfNoRow() {
        val lastRc = cApi.errors.sqlite3ErrCode(rawConnectionPtr)
        if (lastRc != SqliteResultCode.SQLITE_ROW) {
            throwAndroidSqliteException("no row", SqliteResultCode.SQLITE_MISUSE)
        }
    }

    private fun throwIfInvalidColumn(index: Int) {
        if (index !in 0 until statementApi.sqlite3ColumnCount(rawStatementPtr)) {
            throwAndroidSqliteException("column index out of range", SqliteResultCode.SQLITE_RANGE)
        }
    }

    private class StatementPtrClosable(
        val connectionPtr: ConnectionPtrClosable,
        val statementPtr: WasmPtr<SqliteStatement>,
        val isFinalized: AtomicBoolean = atomic(false),
    )

    private class StatementPtrClosableCleanAction(
        val databaseLabel: String,
        val statementPtr: StatementPtrClosable,
        private val cApi: Sqlite3CApi,
        val rootLogger: Logger,
    ) : () -> Unit {
        override fun invoke() {
            val alreadyClosed = statementPtr.isFinalized.getAndSet(true)
            if (!alreadyClosed) {
                onStatementLeaked()
                finalizeStatement(databaseLabel, statementPtr, cApi, rootLogger)
            }
        }

        private fun onStatementLeaked() {
            rootLogger.e {
                "A SQLiteStatement object for database '" +
                        databaseLabel + "' was leaked!  Please fix your application " +
                        "to end transactions in progress properly and to close the database " +
                        "when it is no longer needed."
            }
        }
    }

    private companion object {
        fun finalizeStatement(
            databaseLabel: String,
            closableStatementPtr: StatementPtrClosable,
            cApi: Sqlite3CApi,
            logger: Logger,
        ) {
            if (closableStatementPtr.isFinalized.getAndSet(true)) {
                return
            }

            val rawConnectionPtr = closableStatementPtr.connectionPtr.nativePtr
            val rawStatementPtr = closableStatementPtr.statementPtr
            if (!closableStatementPtr.connectionPtr.isClosed.value) {
                logger.v { "Finalized statement $rawStatementPtr on connection $rawConnectionPtr" }
                val result = cApi.statement.sqlite3Finalize(rawConnectionPtr, rawStatementPtr)
                if (result is Sqlite3Result.Error) {
                    logger.v { "sqlite3_finalize($rawConnectionPtr, $rawStatementPtr) failed: ${result.info}" }
                }
            } else {
                logger.w {
                    "A SQLiteStatement object for database '$databaseLabel' is not finalized at the time the " +
                            "SQLiteConnection is closed"
                }
            }
        }
    }
}
