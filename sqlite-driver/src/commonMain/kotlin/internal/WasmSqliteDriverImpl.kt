/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.driver.internal

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteException
import at.released.wasm.sqlite.driver.WasmSQLiteDriver
import at.released.wasm.sqlite.driver.dsl.OpenFlags
import at.released.wasm.sqlite.driver.dsl.OpenParamsBlock
import at.released.wasm.sqlite.open.helper.WasmPtr
import at.released.wasm.sqlite.open.helper.debug.SqliteErrorLogger
import at.released.wasm.sqlite.open.helper.debug.SqliteStatementLogger
import at.released.wasm.sqlite.open.helper.debug.SqliteStatementLogger.TraceEvent
import at.released.wasm.sqlite.open.helper.debug.SqliteStatementProfileLogger
import at.released.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfig
import at.released.wasm.sqlite.open.helper.embedder.SqliteRuntime
import at.released.wasm.sqlite.open.helper.or
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteConfigParameter
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteDbConfigParameter
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteOpenFlags
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode.Companion.name
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTrace
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_CLOSE
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_PROFILE
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_ROW
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_STMT
import at.released.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3CApi
import at.released.weh.common.api.Logger
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal class WasmSqliteDriverImpl<R : SqliteRuntime>(
    debugConfig: WasmSqliteDebugConfig,
    rootLogger: Logger,
    private val cApi: Sqlite3CApi,
    private val openParams: OpenParamsBlock,
    override val runtime: R,
    private val onClose: AutoCloseable,
) : WasmSQLiteDriver<R>, AutoCloseable by onClose {
    private val logger: Logger = rootLogger.withTag("WasmSqliteDriver")
    private val sqliteErrorLogger = debugConfig.getOrCreateDefault(SqliteErrorLogger)
    private val sqliteStatementLogger = debugConfig.getOrCreateDefault(SqliteStatementLogger)
    private val sqliteStatementProfileLogger = debugConfig.getOrCreateDefault(SqliteStatementProfileLogger)
    private var isSqliteInitialized: Boolean = false
    private val lock = SynchronizedObject()

    override fun open(fileName: String): SQLiteConnection = synchronized(lock) {
        initIfRequiredLocked()
        val connectionPtr: WasmPtr<SqliteDb> = nativeOpen(
            path = fileName,
            enableTrace = sqliteStatementLogger.enabled,
            enableProfile = sqliteStatementProfileLogger.enabled,
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
        logger.v { "initIfRequiredLocked(). Sqlite initialized: $isSqliteInitialized" }
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
        if (sqliteErrorLogger.enabled) {
            cApi.config.sqlite3SetLogger(sqliteErrorLogger.logger)
                .throwOnError("sqliteSetLogger() failed")
        }

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
        logger.v { "nativeOpen($path, $enableTrace, $enableProfile, $lookasideSlotSize, $lookasideSlotCount)" }
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
                    WasmPtr.C_NULL,
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
            logger.v { "closeDatabaseSilent($db)" }
            val closeError = cApi.db.sqlite3Close(database)
            if (closeError != SqliteResultCode.SQLITE_OK) {
                logger.e { "sqlite3Close() failed with error code `${closeError.name}`" }
            }
        }
    }

    private fun sqliteTraceCallback(trace: SqliteTrace) = when (trace) {
        is SqliteTrace.TraceStmt -> {
            val event = TraceEvent.Statement(trace.db, trace.unexpandedSql ?: "")
            sqliteStatementLogger.logger(event)
        }

        is SqliteTrace.TraceClose -> {
            val event = TraceEvent.Close(trace.db)
            sqliteStatementLogger.logger(event)
        }

        is SqliteTrace.TraceProfile -> {
            val sql = cApi.statement.sqlite3ExpandedSql(trace.statement) ?: trace.statement.toString()
            sqliteStatementProfileLogger.logger(trace.db, sql, trace.time)
        }

        is SqliteTrace.TraceRow -> {
            val event = TraceEvent.Row(trace.db, trace.statement)
            sqliteStatementLogger.logger(event)
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
