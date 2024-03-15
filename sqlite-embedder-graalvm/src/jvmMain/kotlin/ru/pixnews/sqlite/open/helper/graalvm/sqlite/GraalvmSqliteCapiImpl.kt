/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("VARIABLE_HAS_PREFIX")

package ru.pixnews.sqlite.open.helper.graalvm.sqlite

import org.graalvm.polyglot.Value
import ru.pixnews.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.sqlite.open.helper.common.api.WasmPtr.Companion.WASM_SIZEOF_PTR
import ru.pixnews.sqlite.open.helper.common.api.WasmPtr.Companion.sqlite3Null
import ru.pixnews.sqlite.open.helper.common.api.isSqlite3Null
import ru.pixnews.sqlite.open.helper.common.api.plus
import ru.pixnews.sqlite.open.helper.graalvm.bindings.SqliteBindings
import ru.pixnews.sqlite.open.helper.graalvm.bindings.SqliteMemoryBindings
import ru.pixnews.sqlite.open.helper.graalvm.ext.asWasmAddr
import ru.pixnews.sqlite.open.helper.graalvm.ext.readNullTerminatedString
import ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.Sqlite3CallbackFunctionIndexes
import ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.Sqlite3CallbackStore
import ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.Sqlite3CallbackStore.Sqlite3ExecCallbackId
import ru.pixnews.sqlite.open.helper.host.memory.write
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteCapi
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteCapi.SqliteDbReadonlyResult
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteCapi.SqliteDbStatusResult
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteColumnType
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteComparatorCallback
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteDbStatusParameter
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteDestructorType.Companion.SQLITE_TRANSIENT
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteErrno
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteErrorInfo
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteException
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteExecCallback
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteOpenFlags
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteResult
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteStatement
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteTextEncoding
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteTraceCallback
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode

internal class GraalvmSqliteCapiImpl internal constructor(
    val sqliteBindings: SqliteBindings,
    val callbackStore: Sqlite3CallbackStore,
    private val callbackFunctionIndexes: Sqlite3CallbackFunctionIndexes,
) : SqliteCapi {
    private val memory: SqliteMemoryBindings = sqliteBindings.memoryBindings

    val sqlite3Version: String
        get() {
            val resultPtr = sqliteBindings.sqlite3_libversion.execute()
            return checkNotNull(memory.memory.readNullTerminatedString(resultPtr))
        }

    val sqlite3SourceId: String
        get() {
            val resultPtr = sqliteBindings.sqlite3_sourceid.execute()
            return checkNotNull(memory.memory.readNullTerminatedString(resultPtr))
        }

    val sqlite3VersionNumber: Int
        get() = sqliteBindings.sqlite3_libversion_number.execute().asInt()

    val sqlite3WasmEnumJson: String?
        get() {
            val resultPtr = sqliteBindings.sqlite3_wasm_enum_json.execute()
            return memory.memory.readNullTerminatedString(resultPtr)
        }

    val sqlite3VersionFull: Sqlite3Version
        get() = Sqlite3Version(
            sqlite3Version,
            sqlite3VersionNumber,
            sqlite3SourceId,
        )

    fun sqlite3Open(
        filename: String,
    ): WasmPtr<SqliteDb> {
        var ppDb: WasmPtr<WasmPtr<SqliteDb>> = sqlite3Null()
        var pFileName: WasmPtr<Byte> = sqlite3Null()
        var pDb: WasmPtr<SqliteDb> = sqlite3Null()
        try {
            ppDb = memory.allocOrThrow(WASM_SIZEOF_PTR)
            pFileName = memory.allocZeroTerminatedString(filename)

            val result: Value = sqliteBindings.sqlite3_open.execute(pFileName.addr, ppDb.addr)

            pDb = memory.readAddr(ppDb)
            result.throwOnSqliteError("sqlite3_open() failed", pDb)

            return pDb
        } catch (@Suppress("TooGenericExceptionCaught") ex: Throwable) {
            sqlite3Close(pDb)
            throw ex
        } finally {
            memory.freeSilent(ppDb)
            memory.freeSilent(pFileName)
        }
    }

    override fun sqlite3OpenV2(
        filename: String,
        flags: SqliteOpenFlags,
        vfsName: String?,
    ): WasmPtr<SqliteDb> {
        var ppDb: WasmPtr<WasmPtr<SqliteDb>> = sqlite3Null()
        var pFileName: WasmPtr<Byte> = sqlite3Null()
        var pVfsName: WasmPtr<Byte> = sqlite3Null()
        var pDb: WasmPtr<SqliteDb> = sqlite3Null()
        try {
            ppDb = memory.allocOrThrow(WASM_SIZEOF_PTR)
            pFileName = memory.allocZeroTerminatedString(filename)
            if (vfsName != null) {
                pVfsName = memory.allocZeroTerminatedString(vfsName)
            }

            val result: Value = sqliteBindings.sqlite3_open_v2.execute(
                pFileName.addr,
                ppDb.addr,
                flags.mask.toInt(),
                pVfsName.addr,
            )

            pDb = memory.readAddr(ppDb)
            result.throwOnSqliteError("sqlite3_open_v2() failed", pDb)

            return pDb
        } catch (@Suppress("TooGenericExceptionCaught") ex: Throwable) {
            sqlite3Close(pDb)
            throw ex
        } finally {
            memory.freeSilent(ppDb)
            memory.freeSilent(pFileName)
            memory.freeSilent(pVfsName)
        }
    }

    override fun sqlite3Close(
        sqliteDb: WasmPtr<SqliteDb>,
    ) {
        // TODO: __dbCleanupMap.cleanup(pDb)
        sqliteBindings.sqlite3_close_v2.execute(sqliteDb.addr)
            .throwOnSqliteError("sqlite3_close_v2() failed", sqliteDb)
    }

    fun sqlite3ErrMsg(
        sqliteDb: WasmPtr<SqliteDb>,
    ): String? {
        val errorAddr = sqliteBindings.sqlite3_errmsg.execute(sqliteDb.addr)
        return memory.readZeroTerminatedString(errorAddr)
    }

    fun sqlite3ErrCode(
        sqliteDb: WasmPtr<SqliteDb>,
    ): Int {
        return sqliteBindings.sqlite3_errcode.execute(sqliteDb.addr).asInt()
    }

    fun sqlite3ExtendedErrCode(
        sqliteDb: WasmPtr<SqliteDb>,
    ): Int {
        return sqliteBindings.sqlite3_extended_errcode.execute(sqliteDb.addr).asInt()
    }

    override fun sqlite3CreateCollation(
        database: WasmPtr<SqliteDb>,
        name: String,
        comparator: SqliteComparatorCallback?,
    ) {
        val pCallbackId: Sqlite3CallbackStore.Sqlite3ComparatorId? = if (comparator != null) {
            callbackStore.sqlite3Comparators.put(comparator)
        } else {
            null
        }

        val pName: WasmPtr<Byte> = memory.allocZeroTerminatedString(name)

        val errNo = sqliteBindings.sqlite3_create_collation_v2.execute(
            database.addr,
            pName.addr,
            SqliteTextEncoding.SQLITE_UTF8.id,
            pCallbackId?.id,
            if (pCallbackId != null) callbackFunctionIndexes.execCallbackFunction.funcId else 0,
            if (pCallbackId != null) callbackFunctionIndexes.destroyComparatorFunction.funcId else 0,
        )
        memory.freeSilent(pName)
        if (errNo.asInt() != Errno.SUCCESS.code && pCallbackId != null) {
            callbackStore.sqlite3Comparators.remove(pCallbackId)
        }
        errNo.throwOnSqliteError("sqlite3CreateCollation() failed", database)
    }

    fun sqlite3Exec(
        database: WasmPtr<SqliteDb>,
        sql: String,
        callback: SqliteExecCallback? = null,
    ): SqliteResult<Unit> {
        var pSql: WasmPtr<Byte> = sqlite3Null()
        var pzErrMsg: WasmPtr<WasmPtr<Byte>> = sqlite3Null()
        val pCallbackId: Sqlite3ExecCallbackId? = if (callback != null) {
            callbackStore.sqlite3ExecCallbacks.put(callback)
        } else {
            null
        }

        try {
            pSql = memory.allocZeroTerminatedString(sql)
            pzErrMsg = memory.allocOrThrow(WASM_SIZEOF_PTR)

            val errNo = sqliteBindings.sqlite3_exec.execute(
                database.addr,
                pSql.addr,
                if (pCallbackId != null) callbackFunctionIndexes.execCallbackFunction.funcId else 0,
                pCallbackId?.id ?: 0,
                pzErrMsg.addr,
            ).asInt()

            if (errNo == Errno.SUCCESS.code) {
                return SqliteResult.Success(Unit)
            } else {
                val errMsgAddr: WasmPtr<Byte> = memory.readAddr(pzErrMsg)
                val errMsg = memory.readZeroTerminatedString(errMsgAddr)
                memory.freeSilent(errMsgAddr)
                return SqliteResult.Error(errNo, errNo, errMsg)
            }
        } finally {
            pCallbackId?.let { callbackStore.sqlite3ExecCallbacks.remove(it) }
            memory.freeSilent(pSql)
            memory.freeSilent(pzErrMsg)
        }
    }

    override fun sqlite3DbReadonly(
        sqliteDb: WasmPtr<SqliteDb>,
        dbName: String?,
    ): SqliteCapi.SqliteDbReadonlyResult {
        val pDbName = if (dbName != null) {
            memory.allocZeroTerminatedString(dbName)
        } else {
            sqlite3Null()
        }

        try {
            val readonlyResultId = sqliteBindings.sqlite3_db_readonly.execute(sqliteDb.addr, pDbName.addr).asInt()
            return SqliteDbReadonlyResult.Companion.fromId(readonlyResultId)
        } finally {
            memory.freeSilent(pDbName)
        }
    }

    override fun sqlite3BusyTimeout(
        sqliteDb: WasmPtr<SqliteDb>,
        ms: Int,
    ) {
        val result = sqliteBindings.sqlite3_busy_timeout.execute(sqliteDb.addr, ms)
        result.throwOnSqliteError("sqlite3BusyTimeout() failed", sqliteDb)
    }

    override fun sqlite3Trace(
        sqliteDb: WasmPtr<SqliteDb>,
        mask: SqliteTraceEventCode,
        traceCallback: SqliteTraceCallback?,
    ) {
        // TODO: remove callback on close
        if (traceCallback != null) {
            callbackStore.sqlite3TraceCallbacks[sqliteDb] = traceCallback
        }

        val errNo = sqliteBindings.sqlite3_trace_v2.execute(
            sqliteDb.addr,
            mask.mask.toInt(),
            if (traceCallback != null) callbackFunctionIndexes.traceFunction.funcId else 0,
            sqliteDb.addr,
        )

        if (traceCallback == null || errNo.asInt() != Errno.SUCCESS.code) {
            callbackStore.sqlite3TraceCallbacks.remove(sqliteDb)
        }

        errNo.throwOnSqliteError("sqlite3_trace_v2() failed", sqliteDb)
    }

    override fun sqlite3ProgressHandler(
        sqliteDb: WasmPtr<SqliteDb>,
        instructions: Int,
        progressCallback: SqliteProgressCallback?,
    ) {
        @Suppress("NULLABLE_PROPERTY_TYPE")
        val activeCallback: SqliteProgressCallback? = if (instructions >= 1) {
            progressCallback
        } else {
            null
        }

        // TODO: remove callback on close
        if (activeCallback != null) {
            callbackStore.sqlite3ProgressCallbacks[sqliteDb] = activeCallback
        }

        val errNo = sqliteBindings.sqlite3_progress_handler.execute(
            sqliteDb.addr,
            instructions,
            if (activeCallback != null) callbackFunctionIndexes.progressFunction.funcId else 0,
            sqliteDb.addr,
        )

        if (activeCallback == null) {
            callbackStore.sqlite3ProgressCallbacks.remove(sqliteDb)
        }

        errNo.throwOnSqliteError("sqlite3ProgressHandler() failed", sqliteDb)
    }

    override fun sqlite3DbStatus(
        sqliteDb: WasmPtr<SqliteDb>,
        op: SqliteDbStatusParameter,
        resetFlag: Boolean,
    ): SqliteDbStatusResult {
        var pCur: WasmPtr<Int> = sqlite3Null()
        var pHiwtr: WasmPtr<Int> = sqlite3Null()

        try {
            pCur = memory.allocOrThrow(4U)
            pHiwtr = memory.allocOrThrow(4U)

            val errCode = sqliteBindings.sqlite3_db_status.execute(
                sqliteDb.addr,
                op.id,
                pCur.addr,
                pHiwtr.addr,
                if (resetFlag) 1 else 0,
            )
            errCode.throwOnSqliteError(null, sqliteDb)
            return SqliteDbStatusResult(0, 0)
        } finally {
            memory.freeSilent(pCur)
            memory.freeSilent(pHiwtr)
        }
    }

    override fun sqlite3ColumnCount(
        statement: WasmPtr<SqliteStatement>,
    ): Int {
        return sqliteBindings.sqlite3_column_count.execute(statement.addr).asInt()
    }

    override fun sqlite3ColumnText(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): String? {
        val ptr: WasmPtr<Byte> = sqliteBindings.sqlite3_column_text.execute(
            statement.addr,
            columnIndex,
        ).asWasmAddr()
        return memory.readZeroTerminatedString(ptr)
    }

    override fun sqlite3ColumnInt64(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): Long {
        return sqliteBindings.sqlite3_column_int64.execute(
            statement.addr,
            columnIndex,
        ).asLong()
    }

    override fun sqlite3ColumnDouble(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): Double {
        return sqliteBindings.sqlite3_column_double.execute(
            statement.addr,
            columnIndex,
        ).asDouble()
    }

    override fun sqlite3ColumnBlob(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): ByteArray {
        val ptr: WasmPtr<Byte> = sqliteBindings.sqlite3_column_text.execute(
            statement.addr,
            columnIndex,
        ).asWasmAddr()
        val bytes = sqliteBindings.sqlite3_column_bytes.execute(
            statement.addr,
            columnIndex,
        ).asInt()
        return memory.memory.readBytes(ptr, bytes)
    }

    override fun sqlite3Step(
        statement: WasmPtr<SqliteStatement>,
    ): SqliteErrno {
        val errCode = sqliteBindings.sqlite3_step.execute(statement.addr).asInt()
        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    override fun sqlite3Reset(
        statement: WasmPtr<SqliteStatement>,
    ): SqliteErrno {
        val errCode = sqliteBindings.sqlite3_reset.execute(statement.addr).asInt()
        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    override fun sqlite3ColumnType(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): SqliteColumnType {
        val type = sqliteBindings.sqlite3_column_type.execute(statement.addr, columnIndex).asInt()
        return SqliteColumnType(type)
    }

    override fun readSqliteErrorInfo(
        sqliteDb: WasmPtr<SqliteDb>,
    ): SqliteErrorInfo {
        if (sqliteDb.isSqlite3Null()) {
            return SqliteErrorInfo(Errno.SUCCESS.code, Errno.SUCCESS.code, null)
        }

        val errCode = sqlite3ErrCode(sqliteDb)
        val extendedErrCode = sqlite3ExtendedErrCode(sqliteDb)
        val errMsg = if (errCode != 0) {
            sqlite3ErrMsg(sqliteDb) ?: "null"
        } else {
            null
        }
        return SqliteErrorInfo(errCode, extendedErrCode, errMsg)
    }

    private fun Value.throwOnSqliteError(
        msgPrefix: String?,
        sqliteDb: WasmPtr<SqliteDb>,
    ) {
        val errNo = this.asInt()
        if (errNo != Errno.SUCCESS.code) {
            val errInfo = readSqliteErrorInfo(sqliteDb)
            throw SqliteException(errInfo, msgPrefix)
        }
    }

    override fun sqlite3Changes(sqliteDb: WasmPtr<SqliteDb>): Int {
        return sqliteBindings.sqlite3_changes.execute(sqliteDb.addr).asInt()
    }

    override fun sqlite3LastInsertRowId(
        sqliteDb: WasmPtr<SqliteDb>,
    ): Long {
        return sqliteBindings.sqlite3_last_insert_rowid.execute(sqliteDb.addr).asLong()
    }

    override fun sqlite3ClearBindings(statement: WasmPtr<SqliteStatement>): SqliteErrno {
        val errCode = sqliteBindings.sqlite3_clear_bindings.execute(statement.addr).asInt()
        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    override fun sqlite3BindBlobTransient(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
        value: ByteArray,
    ): SqliteErrno {
        val pValue: WasmPtr<Byte> = memory.allocOrThrow(value.size.toUInt())
        memory.memory.write(pValue, value, 0, value.size)
        val errCode = try {
            sqliteBindings.sqlite3_bind_blob.execute(
                sqliteDb.addr,
                index,
                pValue.addr,
                value.size,
                SQLITE_TRANSIENT.id, // TODO: change to destructor?
            ).asInt()
        } finally {
            memory.freeSilent(pValue)
        }

        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    override fun sqlite3BindStringTransient(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
        value: String,
    ): SqliteErrno {
        val encoded = value.encodeToByteArray()
        val size = encoded.size

        val pValue: WasmPtr<Byte> = memory.allocOrThrow(size.toUInt())
        memory.memory.write(pValue, encoded, 0, size)
        val errCode = try {
            sqliteBindings.sqlite3_bind_text.execute(
                sqliteDb.addr,
                index,
                pValue.addr,
                size,
                SQLITE_TRANSIENT.id, // TODO: change to destructor?
            ).asInt()
        } finally {
            memory.freeSilent(pValue)
        }

        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    override fun sqlite3BindDouble(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
        value: Double,
    ): SqliteErrno {
        val errCode = sqliteBindings.sqlite3_bind_double.execute(
            sqliteDb.addr,
            index,
            value,
        ).asInt()
        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    override fun sqlite3BindLong(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
        value: Long,
    ): SqliteErrno {
        val errCode = sqliteBindings.sqlite3_bind_int64.execute(
            sqliteDb.addr,
            index,
            value,
        ).asInt()
        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    override fun sqlite3BindNull(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
    ): SqliteErrno {
        val errCode = sqliteBindings.sqlite3_bind_int64.execute(sqliteDb.addr, index).asInt()
        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    override fun sqlite3ColumnName(
        statement: WasmPtr<SqliteStatement>,
        index: Int,
    ): String? {
        val ptr: WasmPtr<Byte> = sqliteBindings.sqlite3_column_name.execute(statement.addr, index).asWasmAddr()
        return memory.readZeroTerminatedString(ptr)
    }

    override fun sqlite3StmtReadonly(statement: WasmPtr<SqliteStatement>): Boolean {
        return sqliteBindings.sqlite3_stmt_readonly.execute(statement.addr).asInt() != 0
    }

    override fun sqlite3BindParameterCount(statement: WasmPtr<SqliteStatement>): Int {
        return sqliteBindings.sqlite3_bind_parameter_count.execute(statement.addr).asInt()
    }

    override fun sqlite3PrepareV2(
        sqliteDb: WasmPtr<SqliteDb>,
        sql: String,
    ): WasmPtr<SqliteStatement> {
        var sqlBytesPtr: WasmPtr<Byte> = sqlite3Null()
        var ppStatement: WasmPtr<WasmPtr<SqliteStatement>> = sqlite3Null()

        try {
            val sqlEncoded = sql.encodeToByteArray()
            val nullTerminatedSqlSize = sqlEncoded.size + 1

            sqlBytesPtr = memory.allocOrThrow(nullTerminatedSqlSize.toUInt())
            ppStatement = memory.allocOrThrow(WASM_SIZEOF_PTR)

            memory.memory.write(sqlBytesPtr, sqlEncoded)
            memory.memory.writeByte(sqlBytesPtr + sqlEncoded.size, 0)

            val result = sqliteBindings.sqlite3_prepare_v2.execute(
                sqliteDb.addr,
                sqlBytesPtr.addr,
                nullTerminatedSqlSize,
                ppStatement.addr,
                sqlite3Null<Unit>().addr,
            )
            result.throwOnSqliteError("sqlite3_prepare_v2() failed", sqliteDb)
            return memory.readAddr(ppStatement)
        } finally {
            memory.freeSilent(sqlBytesPtr)
            memory.freeSilent(ppStatement)
        }
    }

    override fun sqlite3Finalize(
        sqliteDatabase: WasmPtr<SqliteDb>,
        statement: WasmPtr<SqliteStatement>,
    ) {
        val errCode = sqliteBindings.sqlite3_finalize.execute(statement.addr)
        errCode.throwOnSqliteError("sqlite3_finalize() failed", sqliteDatabase)
    }

    override fun sqlite3ExpandedSql(statement: WasmPtr<SqliteStatement>): String? {
        val ptr = sqliteBindings.sqlite3_expanded_sql.execute(statement.addr)
        return memory.readZeroTerminatedString(ptr)
    }

    data class Sqlite3Version(
        val version: String,
        val versionNumber: Int,
        val sourceId: String,
    )
}
