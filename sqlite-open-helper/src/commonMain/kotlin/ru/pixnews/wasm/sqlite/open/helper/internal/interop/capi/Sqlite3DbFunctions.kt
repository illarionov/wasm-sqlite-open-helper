/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.interop.capi

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.EmbedderMemory
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readPtr
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.allocZeroTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.sqliteFreeSilent
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.SqliteDatabaseResourcesRegistry
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.capi.Sqlite3ErrorFunctions.Companion.createSqlite3Result
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.capi.Sqlite3Result.Error
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.capi.Sqlite3Result.Success
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDbConfigParameter
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDbStatusParameter
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteOpenFlags
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode.Companion.SQLITE_OK
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode.Companion.name
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode

internal class Sqlite3DbFunctions(
    private val sqliteBindings: SqliteBindings,
    private val memory: EmbedderMemory,
    private val callbackStore: SqliteCallbackStore,
    private val callbackFunctionIndexes: Sqlite3CallbackFunctionIndexes,
    private val databaseResources: SqliteDatabaseResourcesRegistry,
    private val sqliteErrorApi: Sqlite3ErrorFunctions,
    rootLogger: Logger,
) {
    private val logger = rootLogger.withTag("Sqlite3DbFunctions")
    private val memoryBindings = sqliteBindings.memoryBindings

    fun sqlite3OpenV2(
        filename: String,
        flags: SqliteOpenFlags,
        vfsName: String?,
    ): Sqlite3Result<WasmPtr<SqliteDb>> {
        var ppDb: WasmPtr<WasmPtr<SqliteDb>> = WasmPtr.sqlite3Null()
        var pFileName: WasmPtr<Byte> = WasmPtr.sqlite3Null()
        var pVfsName: WasmPtr<Byte> = WasmPtr.sqlite3Null()
        try {
            ppDb = memoryBindings.sqliteAllocOrThrow(WasmPtr.WASM_SIZEOF_PTR)
            pFileName = memoryBindings.allocZeroTerminatedString(memory, filename)
            if (vfsName != null) {
                pVfsName = memoryBindings.allocZeroTerminatedString(memory, vfsName)
            }

            val resultCode = sqliteBindings.sqlite3_open_v2.executeForSqliteResultCode(
                pFileName.addr,
                ppDb.addr,
                flags.mask.toInt(),
                pVfsName.addr,
            )
            val pDb: WasmPtr<SqliteDb> = memory.readPtr(ppDb)
            val result = sqliteErrorApi.createSqlite3Result(resultCode, pDb, pDb)

            when (result) {
                is Success -> databaseResources.onDbOpened(pDb)
                is Error -> sqlite3Close(pDb).also { closeError ->
                    if (closeError != SQLITE_OK) {
                        logger.e {
                            "sqlite3Close() failed with error code `${closeError.name}` after opening database failed"
                        }
                    }
                }
            }
            return result
        } finally {
            memoryBindings.sqliteFreeSilent(ppDb)
            memoryBindings.sqliteFreeSilent(pFileName)
            memoryBindings.sqliteFreeSilent(pVfsName)
        }
    }

    fun sqlite3Close(
        sqliteDb: WasmPtr<SqliteDb>,
    ): SqliteResultCode {
        return try {
            sqliteBindings.sqlite3_close_v2.executeForSqliteResultCode(sqliteDb.addr)
        } finally {
            databaseResources.afterDbClosed(sqliteDb)
        }
    }

   fun sqlite3DbReadonly(
        sqliteDb: WasmPtr<SqliteDb>,
        dbName: String?,
    ): SqliteDbReadonlyResult {
        val pDbName = if (dbName != null) {
            memoryBindings.allocZeroTerminatedString(memory, dbName)
        } else {
            WasmPtr.sqlite3Null()
        }

        try {
            val readonlyResultId = sqliteBindings.sqlite3_db_readonly.executeForInt(sqliteDb.addr, pDbName.addr)
            return SqliteDbReadonlyResult.fromId(readonlyResultId)
        } finally {
            memoryBindings.sqliteFreeSilent(pDbName)
        }
    }

    fun sqlite3BusyTimeout(
        sqliteDb: WasmPtr<SqliteDb>,
        ms: Int,
    ): Sqlite3Result<Unit> {
        val errCode = sqliteBindings.sqlite3_busy_timeout.executeForSqliteResultCode(sqliteDb.addr, ms)
        return sqliteErrorApi.createSqlite3Result(errCode, Unit, sqliteDb)
    }

    fun sqlite3DbConfig(
        sqliteDb: WasmPtr<SqliteDb>,
        op: SqliteDbConfigParameter,
        pArg1: WasmPtr<*>,
        arg2: Int,
        arg3: Int,
    ): Sqlite3Result<Unit> {
        val errCode = sqliteBindings.sqlite3__wasm_db_config_pii.executeForSqliteResultCode(
            sqliteDb.addr,
            op.id,
            pArg1.addr,
            arg2,
            arg3,
        )
        return sqliteErrorApi.createSqlite3Result(errCode, Unit, sqliteDb)
    }

    fun sqlite3Trace(
        sqliteDb: WasmPtr<SqliteDb>,
        mask: SqliteTraceEventCode,
        traceCallback: SqliteTraceCallback?,
    ): Sqlite3Result<Unit> {
        if (traceCallback != null) {
            callbackStore.sqlite3TraceCallbacks[sqliteDb] = traceCallback
        }

        val errCode = sqliteBindings.sqlite3_trace_v2.executeForSqliteResultCode(
            sqliteDb.addr,
            mask.mask.toInt(),
            if (traceCallback != null) callbackFunctionIndexes.traceFunction.funcId else 0,
            sqliteDb.addr,
        )

        if (traceCallback == null || errCode != SQLITE_OK) {
            callbackStore.sqlite3TraceCallbacks.remove(sqliteDb)
        }

        return sqliteErrorApi.createSqlite3Result(errCode, Unit, sqliteDb)
    }

    fun registerAndroidFunctions(sqliteDb: WasmPtr<SqliteDb>): Sqlite3Result<Unit> {
        val errCode = sqliteBindings.register_android_functions.executeForSqliteResultCode(
            sqliteDb.addr,
            0, // utf16Storage
        )
        return sqliteErrorApi.createSqlite3Result(errCode, Unit, sqliteDb)
    }

    fun registerLocalizedCollators(
        sqliteDb: WasmPtr<SqliteDb>,
        newLocale: String,
    ): Sqlite3Result<Unit> {
        var pNewLocale: WasmPtr<Byte> = WasmPtr.sqlite3Null()
        try {
            pNewLocale = memoryBindings.allocZeroTerminatedString(memory, newLocale)
            val errCode = sqliteBindings.register_localized_collators.executeForSqliteResultCode(
                sqliteDb.addr,
                pNewLocale.addr,
                0, // utf16Storage
            )
            return sqliteErrorApi.createSqlite3Result(errCode, Unit, sqliteDb)
        } finally {
            memoryBindings.sqliteFreeSilent(pNewLocale)
        }
    }

    fun sqlite3ProgressHandler(
        sqliteDb: WasmPtr<SqliteDb>,
        instructions: Int,
        progressCallback: SqliteProgressCallback?,
    ): Sqlite3Result<Unit> {
        @Suppress("NULLABLE_PROPERTY_TYPE")
        val activeCallback: SqliteProgressCallback? = if (instructions >= 1) {
            progressCallback
        } else {
            null
        }

        if (activeCallback != null) {
            callbackStore.sqlite3ProgressCallbacks[sqliteDb] = activeCallback
        }

        val errNo = sqliteBindings.sqlite3_progress_handler.executeForSqliteResultCode(
            sqliteDb.addr,
            instructions,
            if (activeCallback != null) callbackFunctionIndexes.progressFunction.funcId else 0,
            sqliteDb.addr,
        )

        if (activeCallback == null) {
            callbackStore.sqlite3ProgressCallbacks.remove(sqliteDb)
        }

        return sqliteErrorApi.createSqlite3Result(errNo, Unit, sqliteDb)
    }

    fun sqlite3DbStatus(
        sqliteDb: WasmPtr<SqliteDb>,
        op: SqliteDbStatusParameter,
        resetFlag: Boolean,
    ): Sqlite3Result<SqliteDbStatusResult> {
        var pCur: WasmPtr<Int> = WasmPtr.sqlite3Null()
        var pHiwtr: WasmPtr<Int> = WasmPtr.sqlite3Null()

        try {
            pCur = memoryBindings.sqliteAllocOrThrow(4U)
            pHiwtr = memoryBindings.sqliteAllocOrThrow(4U)

            val errCode = sqliteBindings.sqlite3_db_status.executeForSqliteResultCode(
                sqliteDb.addr,
                op.id,
                pCur.addr,
                pHiwtr.addr,
                if (resetFlag) 1 else 0,
            )
            // TODO: check
            val result = if (errCode == SQLITE_OK) {
                SqliteDbStatusResult(
                    memory.readI32(pCur),
                    memory.readI32(pHiwtr),
                )
            } else {
                SqliteDbStatusResult(0, 0)
            }
            return sqliteErrorApi.createSqlite3Result(errCode, result, sqliteDb)
        } finally {
            memoryBindings.sqliteFreeSilent(pCur)
            memoryBindings.sqliteFreeSilent(pHiwtr)
        }
    }

    fun sqlite3Changes(sqliteDb: WasmPtr<SqliteDb>): Int {
        return sqliteBindings.sqlite3_changes.executeForInt(sqliteDb.addr)
    }

    fun sqlite3LastInsertRowId(
        sqliteDb: WasmPtr<SqliteDb>,
    ): Long {
        return sqliteBindings.sqlite3_last_insert_rowid.executeForLong(sqliteDb.addr)
    }

    internal enum class SqliteDbReadonlyResult(public val id: Int) {
        READ_ONLY(1),
        READ_WRITE(0),
        INVALID_NAME(-1),
        ;

        public companion object {
            public fun fromId(id: Int): SqliteDbReadonlyResult = entries.first { it.id == id }
         }
     }

    internal class SqliteDbStatusResult(
        public val current: Int,
        public val highestInstantaneousValue: Int,
    )
}
