/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.sqlite.common.capi

import at.released.wasm.sqlite.open.helper.WasmPtr
import at.released.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import at.released.wasm.sqlite.open.helper.embedder.exports.SqliteExports
import at.released.wasm.sqlite.open.helper.embedder.exports.allocNullTerminatedString
import at.released.wasm.sqlite.open.helper.embedder.exports.sqliteFreeSilent
import at.released.wasm.sqlite.open.helper.embedder.functiontable.SqliteCallbackFunctionIndexes
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteDbConfigParameter
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteDbStatusParameter
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteOpenFlags
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode.Companion.SQLITE_OK
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode.Companion.name
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceCallback
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode
import at.released.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3ErrorFunctions.Companion.createSqlite3Result
import at.released.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3Result.Error
import at.released.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3Result.Success
import at.released.wasm.sqlite.open.helper.sqlite.common.capi.databaseresources.SqliteDatabaseResourcesRegistry
import at.released.weh.common.api.Logger
import at.released.weh.wasm.core.memory.Memory
import at.released.weh.wasm.core.memory.readPtr

public class Sqlite3DbFunctions internal constructor(
    private val sqliteExports: SqliteExports,
    private val memory: Memory,
    private val callbackStore: SqliteCallbackStore,
    private val callbackFunctionIndexes: SqliteCallbackFunctionIndexes,
    private val databaseResources: SqliteDatabaseResourcesRegistry,
    private val sqliteErrorApi: Sqlite3ErrorFunctions,
    rootLogger: Logger,
) {
    private val logger = rootLogger.withTag("Sqlite3DbFunctions")
    private val memoryBindings = sqliteExports.memoryExports

    public fun sqlite3OpenV2(
        filename: String,
        flags: SqliteOpenFlags,
        vfsName: String?,
    ): Sqlite3Result<WasmPtr<SqliteDb>> {
        var ppDb: WasmPtr<WasmPtr<SqliteDb>> = WasmPtr.cNull()
        var pFileName: WasmPtr<Byte> = WasmPtr.cNull()
        var pVfsName: WasmPtr<Byte> = WasmPtr.cNull()

        logger.v { "sqlite3OpenV2($filename, $flags, $vfsName)" }

        try {
            ppDb = memoryBindings.sqliteAllocOrThrow(WasmPtr.WASM_SIZEOF_PTR)
            pFileName = memoryBindings.allocNullTerminatedString(memory, filename)
            if (vfsName != null) {
                pVfsName = memoryBindings.allocNullTerminatedString(memory, vfsName)
            }

            val resultCode = sqliteExports.sqlite3_open_v2.executeForSqliteResultCode(
                pFileName.addr,
                ppDb.addr,
                flags.mask.toInt(),
                pVfsName.addr,
            )
            val pDb: WasmPtr<SqliteDb> = WasmPtr(memory.readPtr(ppDb.addr))
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

    public fun sqlite3Close(
        sqliteDb: WasmPtr<SqliteDb>,
    ): SqliteResultCode {
        logger.v { "sqlite3Close($sqliteDb)" }
        return try {
            sqliteExports.sqlite3_close_v2.executeForSqliteResultCode(sqliteDb.addr)
        } finally {
            databaseResources.afterDbClosed(sqliteDb)
        }
    }

    public fun sqlite3DbReadonly(
        sqliteDb: WasmPtr<SqliteDb>,
        dbName: String?,
    ): SqliteDbReadonlyResult {
        val pDbName = if (dbName != null) {
            memoryBindings.allocNullTerminatedString(memory, dbName)
        } else {
            WasmPtr.cNull()
        }

        try {
            val readonlyResultId = sqliteExports.sqlite3_db_readonly.executeForInt(sqliteDb.addr, pDbName.addr)
            return SqliteDbReadonlyResult.fromId(readonlyResultId)
        } finally {
            memoryBindings.sqliteFreeSilent(pDbName)
        }
    }

    public fun sqlite3BusyTimeout(
        sqliteDb: WasmPtr<SqliteDb>,
        ms: Int,
    ): Sqlite3Result<Unit> {
        val errCode = sqliteExports.sqlite3_busy_timeout.executeForSqliteResultCode(sqliteDb.addr, ms)
        return sqliteErrorApi.createSqlite3Result(errCode, Unit, sqliteDb)
    }

    public fun sqlite3DbConfig(
        sqliteDb: WasmPtr<SqliteDb>,
        op: SqliteDbConfigParameter,
        pArg1: WasmPtr<*>,
        arg2: Int,
        arg3: Int,
    ): Sqlite3Result<Unit> {
        val errCode = sqliteExports.sqlite3__wasm_db_config_pii.executeForSqliteResultCode(
            sqliteDb.addr,
            op.id,
            pArg1.addr,
            arg2,
            arg3,
        )
        return sqliteErrorApi.createSqlite3Result(errCode, Unit, sqliteDb)
    }

    public fun sqlite3Trace(
        sqliteDb: WasmPtr<SqliteDb>,
        mask: SqliteTraceEventCode,
        traceCallback: SqliteTraceCallback?,
    ): Sqlite3Result<Unit> {
        if (traceCallback != null) {
            callbackStore.sqlite3TraceCallbacks[sqliteDb] = traceCallback
        }

        val errCode = sqliteExports.sqlite3_trace_v2.executeForSqliteResultCode(
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

    public fun registerAndroidFunctions(sqliteDb: WasmPtr<SqliteDb>): Sqlite3Result<Unit> {
        val errCode = sqliteExports.register_android_functions.executeForSqliteResultCode(
            sqliteDb.addr,
            0, // utf16Storage
        )
        return sqliteErrorApi.createSqlite3Result(errCode, Unit, sqliteDb)
    }

    public fun registerLocalizedCollators(
        sqliteDb: WasmPtr<SqliteDb>,
        newLocale: String,
    ): Sqlite3Result<Unit> {
        var pNewLocale: WasmPtr<Byte> = WasmPtr.cNull()
        try {
            pNewLocale = memoryBindings.allocNullTerminatedString(memory, newLocale)
            val errCode = sqliteExports.register_localized_collators.executeForSqliteResultCode(
                sqliteDb.addr,
                pNewLocale.addr,
                0, // utf16Storage
            )
            return sqliteErrorApi.createSqlite3Result(errCode, Unit, sqliteDb)
        } finally {
            memoryBindings.sqliteFreeSilent(pNewLocale)
        }
    }

    public fun sqlite3ProgressHandler(
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

        val errNo = sqliteExports.sqlite3_progress_handler.executeForSqliteResultCode(
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

    public fun sqlite3DbStatus(
        sqliteDb: WasmPtr<SqliteDb>,
        op: SqliteDbStatusParameter,
        resetFlag: Boolean,
    ): Sqlite3Result<SqliteDbStatusResult> {
        var pCur: WasmPtr<Int> = WasmPtr.cNull()
        var pHiwtr: WasmPtr<Int> = WasmPtr.cNull()

        try {
            pCur = memoryBindings.sqliteAllocOrThrow(4U)
            pHiwtr = memoryBindings.sqliteAllocOrThrow(4U)

            val errCode = sqliteExports.sqlite3_db_status.executeForSqliteResultCode(
                sqliteDb.addr,
                op.id,
                pCur.addr,
                pHiwtr.addr,
                if (resetFlag) 1 else 0,
            )
            // TODO: check
            val result = if (errCode == SQLITE_OK) {
                SqliteDbStatusResult(
                    memory.readI32(pCur.addr),
                    memory.readI32(pHiwtr.addr),
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

    public fun sqlite3Changes(sqliteDb: WasmPtr<SqliteDb>): Int {
        return sqliteExports.sqlite3_changes.executeForInt(sqliteDb.addr)
    }

    public fun sqlite3LastInsertRowId(
        sqliteDb: WasmPtr<SqliteDb>,
    ): Long {
        return sqliteExports.sqlite3_last_insert_rowid.executeForLong(sqliteDb.addr)
    }

    public enum class SqliteDbReadonlyResult(public val id: Int) {
        READ_ONLY(1),
        READ_WRITE(0),
        INVALID_NAME(-1),
        ;

        public companion object {
            public fun fromId(id: Int): SqliteDbReadonlyResult = entries.first { it.id == id }
         }
     }

    public class SqliteDbStatusResult(
        public val current: Int,
        public val highestInstantaneousValue: Int,
    )
}
