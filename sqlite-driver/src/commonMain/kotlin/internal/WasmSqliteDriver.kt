/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.internal

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteException
import ru.pixnews.wasm.sqlite.driver.dsl.OpenFlags
import ru.pixnews.wasm.sqlite.driver.dsl.OpenParamsBlock
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.or
import ru.pixnews.wasm.sqlite.open.helper.io.lock.SynchronizedObject
import ru.pixnews.wasm.sqlite.open.helper.io.lock.synchronized
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteConfigParameter
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDbConfigParameter
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteOpenFlags
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode.Companion.name
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTrace
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_CLOSE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_PROFILE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_ROW
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_STMT
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3CApi

internal class WasmSqliteDriver(
    private val debugConfig: SqliteDebug,
    rootLogger: Logger,
    private val cApi: Sqlite3CApi,
    private val openParams: OpenParamsBlock,
) : SQLiteDriver {
    private val logger: Logger = rootLogger.withTag("WasmSqliteDriver")
    private var isSqliteInitialized: Boolean = false
    private val lock = SynchronizedObject()

    override fun open(fileName: String): SQLiteConnection = synchronized(lock) {
        initIfRequiredLocked()
        val connectionPtr: WasmPtr<SqliteDb> = nativeOpen(
            path = fileName,
            enableTrace = debugConfig.sqlStatements,
            enableProfile = debugConfig.sqlTime,
            lookasideSlotSize = openParams.lookasideSlotSize,
            lookasideSlotCount = openParams.lookasideSlotCount,
        )

        val connection = WasmSqliteConnection(
            databaseLabel = fileName,
            connectionPtr = connectionPtr,
            cApi = cApi,
            rootLogger = logger,
            openParams = openParams,
        )
        try {
            connection.configure()
            return connection
        } catch (ex: SQLiteException) {
            connection.close()
            throw ex
        }
    }

    /**
     * Initializes the SQLite library.
     *
     * Executes [sqlite3_initialize](https://www.sqlite.org/c3ref/initialize.html) and a set of routines that
     * can only be called before it.
     */
    private fun initIfRequiredLocked() {
        if (isSqliteInitialized) {
            return
        } else {
            isSqliteInitialized = true
        }

        if (cApi.embedderInfo.supportMultithreading) {
            // Enable multi-threaded mode.  In this mode, SQLite is safe to use by multiple
            // threads as long as no two threads use the same database connection at the same
            // time (which we guarantee in the SQLite database wrappers).
            cApi.config.sqlite3Config(SqliteConfigParameter.SQLITE_CONFIG_MULTITHREAD, 1)
                .throwOnError("sqlite3__wasm_config_i() failed")
        }

        // Redirect SQLite log messages to the log.
        val sqliteLogger = logger.withTag("sqlite3")
        cApi.config.sqlite3SetLogger { errCode, message -> sqliteLogger.w { "$errCode: $message" } }
            .throwOnError("sqliteSetLogger() failed")

        // The soft heap limit prevents the page cache allocations from growing
        // beyond the given limit, no matter what the max page cache sizes are
        // set to. The limit does not, as of 3.5.0, affect any other allocations.
        cApi.config.sqlite3SoftHeapLimit(SOFT_HEAP_LIMIT)
            .throwOnError("sqlite3_soft_heap_limit64() failed")

        cApi.config.sqlite3initialize()
            .throwOnError("sqlite3_initialize failed")
    }

    private fun nativeOpen(
        path: String,
        enableTrace: Boolean,
        enableProfile: Boolean,
        lookasideSlotSize: Int,
        lookasideSlotCount: Int,
    ): WasmPtr<SqliteDb> {
        var db: WasmPtr<SqliteDb>? = null
        try {
            db = cApi.db.sqlite3OpenV2(
                filename = path,
                flags = SqliteOpenFlags.SQLITE_OPEN_READWRITE or SqliteOpenFlags.SQLITE_OPEN_CREATE,
                vfsName = null,
            ).getOrThrow("sqlite3OpenV2() failed")

            if (lookasideSlotSize > 0 && lookasideSlotCount > 0) {
                cApi.db.sqlite3DbConfig(
                    db,
                    SqliteDbConfigParameter.SQLITE_DBCONFIG_LOOKASIDE,
                    WasmPtr.SQLITE3_NULL,
                    lookasideSlotSize,
                    lookasideSlotCount,
                ).getOrThrow("sqlite3DbConfig() failed")
            }

            if (openParams.openFlags.contains(OpenFlags.ANDROID_FUNCTIONS)) {
                // Register custom Android functions.
                cApi.db.registerAndroidFunctions(db)
                    .getOrThrow("register_android_functions() failed")
            }

            // Enable tracing and profiling if requested.
            if (enableTrace || enableProfile) {
                var mask = SqliteTraceEventCode(0U)
                if (enableTrace) {
                    mask = mask or SQLITE_TRACE_STMT or SQLITE_TRACE_ROW or SQLITE_TRACE_CLOSE
                }
                if (enableProfile) {
                    mask = mask or SQLITE_TRACE_PROFILE
                }
                cApi.db.sqlite3Trace(db, mask, ::sqliteTraceCallback)
            }
            return db
        } catch (@Suppress("TooGenericExceptionCaught") otherException: RuntimeException) {
            closeDatabaseSilent(db)
            throw otherException
        }
    }

    private fun closeDatabaseSilent(db: WasmPtr<SqliteDb>?) {
        db?.let { database ->
            val closeError = cApi.db.sqlite3Close(database)
            if (closeError != SqliteResultCode.SQLITE_OK) {
                logger.e { "sqlite3Close() failed with error code `${closeError.name}`" }
            }
        }
    }

    private fun sqliteTraceCallback(trace: SqliteTrace) {
        when (trace) {
            is SqliteTrace.TraceStmt -> logger.d { """${trace.db}: "${trace.unexpandedSql}"""" }
            is SqliteTrace.TraceClose -> logger.d { """${trace.db} closed""" }
            is SqliteTrace.TraceProfile -> logger.d {
                val sql = cApi.statement.sqlite3ExpandedSql(trace.statement) ?: trace.statement.toString()
                """${trace.db}: "$sql" took ${trace.time}"""
            }

            is SqliteTrace.TraceRow -> logger.v { """${trace.db} / ${trace.statement}: Received row""" }
        }
    }

    private companion object {
        /**
         * Amount of heap memory that will be by all database connections within a single process.
         *
         * Set to 8MB in AOSP.
         *
         * https://www.sqlite.org/c3ref/hard_heap_limit64.html
         */
        // XXX: copy from SQLiteGlobal
        public const val SOFT_HEAP_LIMIT: Long = 8 * 1024 * 1024
    }
}
