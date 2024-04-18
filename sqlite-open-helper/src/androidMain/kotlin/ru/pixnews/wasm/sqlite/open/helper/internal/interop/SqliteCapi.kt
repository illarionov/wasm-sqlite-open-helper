/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("VARIABLE_HAS_PREFIX")

package ru.pixnews.wasm.sqlite.open.helper.internal.interop

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.isSqlite3Null
import ru.pixnews.wasm.sqlite.open.helper.common.api.plus
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.EmbedderMemory
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readNullableZeroTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readZeroTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.write
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.writeZeroTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteMemoryBindings
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.sqliteFreeSilent
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore.SqliteComparatorId
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore.SqliteExecCallbackId
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteComparatorCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteConfigParameter
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDbConfigParameter
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDbStatusParameter
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDestructorType
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrno
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrorInfo
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteException
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteExecCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteLogCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteOpenFlags
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteStatement
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTextEncoding
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode

@Suppress("LargeClass")
internal class SqliteCapi internal constructor(
    private val sqliteBindings: SqliteBindings,
    val memory: EmbedderMemory,
    private val callbackStore: JvmSqliteCallbackStore,
    private val callbackFunctionIndexes: Sqlite3CallbackFunctionIndexes,
    rootLogger: Logger,
) {
    private val databaseResources: SqliteOpenDatabaseResources = SqliteOpenDatabaseResources(callbackStore, rootLogger)
    private val memoryBindings: SqliteMemoryBindings = sqliteBindings.memoryBindings

    val sqlite3Version: String
        get() {
            val resultPtr: WasmPtr<Byte> = sqliteBindings.sqlite3_libversion.executeForPtr()
            return checkNotNull(memory.readNullableZeroTerminatedString(resultPtr))
        }

    val sqlite3SourceId: String
        get() {
            val resultPtr: WasmPtr<Byte> = sqliteBindings.sqlite3_sourceid.executeForPtr()
            return checkNotNull(memory.readNullableZeroTerminatedString(resultPtr))
        }

    val sqlite3VersionNumber: Int
        get() = sqliteBindings.sqlite3_libversion_number.executeForInt()

    val sqlite3WasmEnumJson: String?
        get() {
            val resultPtr: WasmPtr<Byte> = sqliteBindings.sqlite3__wasm_enum_json.executeForPtr()
            return memory.readNullableZeroTerminatedString(resultPtr)
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
        var ppDb: WasmPtr<WasmPtr<SqliteDb>> = WasmPtr.sqlite3Null()
        var pFileName: WasmPtr<Byte> = WasmPtr.sqlite3Null()
        var pDb: WasmPtr<SqliteDb> = WasmPtr.sqlite3Null()
        try {
            ppDb = memoryBindings.sqliteAllocOrThrow(WasmPtr.WASM_SIZEOF_PTR)
            pFileName = allocZeroTerminatedString(filename)

            val result = sqliteBindings.sqlite3_open.executeForInt(pFileName.addr, ppDb.addr)

            pDb = memory.readPtr(ppDb)
            result.throwOnSqliteError("sqlite3_open() failed", pDb)

            databaseResources.onDbOpened(pDb)

            return pDb
        } catch (@Suppress("TooGenericExceptionCaught") ex: Throwable) {
            sqlite3Close(pDb)
            throw ex
        } finally {
            memoryBindings.sqliteFreeSilent(ppDb)
            memoryBindings.sqliteFreeSilent(pFileName)
        }
    }

    fun sqlite3initialize() {
        val sqliteInitResult = sqliteBindings.sqlite3_initialize.executeForInt()
        if (sqliteInitResult != SqliteErrno.SQLITE_OK.id) {
            throw SqliteException(sqliteInitResult, sqliteInitResult)
        }
    }

    fun sqlite3OpenV2(
        filename: String,
        flags: SqliteOpenFlags,
        vfsName: String?,
    ): WasmPtr<SqliteDb> {
        var ppDb: WasmPtr<WasmPtr<SqliteDb>> = WasmPtr.sqlite3Null()
        var pFileName: WasmPtr<Byte> = WasmPtr.sqlite3Null()
        var pVfsName: WasmPtr<Byte> = WasmPtr.sqlite3Null()
        var pDb: WasmPtr<SqliteDb> = WasmPtr.sqlite3Null()
        try {
            ppDb = memoryBindings.sqliteAllocOrThrow(WasmPtr.WASM_SIZEOF_PTR)
            pFileName = allocZeroTerminatedString(filename)
            if (vfsName != null) {
                pVfsName = allocZeroTerminatedString(vfsName)
            }

            val result = sqliteBindings.sqlite3_open_v2.executeForInt(
                pFileName.addr,
                ppDb.addr,
                flags.mask.toInt(),
                pVfsName.addr,
            )

            pDb = memory.readPtr(ppDb)
            result.throwOnSqliteError("sqlite3_open_v2() failed", pDb)

            databaseResources.onDbOpened(pDb)

            return pDb
        } catch (@Suppress("TooGenericExceptionCaught") ex: Throwable) {
            sqlite3Close(pDb)
            throw ex
        } finally {
            memoryBindings.sqliteFreeSilent(ppDb)
            memoryBindings.sqliteFreeSilent(pFileName)
            memoryBindings.sqliteFreeSilent(pVfsName)
        }
    }

    fun sqlite3Close(
        sqliteDb: WasmPtr<SqliteDb>,
    ) {
        try {
            sqliteBindings.sqlite3_close_v2.executeForInt(sqliteDb.addr)
                .throwOnSqliteError("sqlite3_close_v2() failed", sqliteDb)
        } finally {
            databaseResources.afterDbClosed(sqliteDb)
        }
    }

    @Suppress("WRONG_OVERLOADING_FUNCTION_ARGUMENTS")
    fun sqlite3Config(op: SqliteConfigParameter, arg1: Int) {
        val errNo = sqliteBindings.sqlite3__wasm_config_i.executeForInt(op.id, arg1)
        if (errNo != Errno.SUCCESS.code) {
            throw SqliteException(errNo, errNo, "sqlite3__wasm_config_i() failed")
        }
    }

    fun sqlite3Config(op: SqliteConfigParameter, arg1: Int, arg2: Int) {
        val errNo = sqliteBindings.sqlite3__wasm_config_ii.executeForInt(op.id, arg1, arg2)
        if (errNo != Errno.SUCCESS.code) {
            throw SqliteException(errNo, errNo, "sqlite3__wasm_config_ii() failed")
        }
    }

    fun sqlite3Config(op: SqliteConfigParameter, arg1: Long) {
        val errNo = sqliteBindings.sqlite3__wasm_config_j.executeForInt(op.id, arg1)
        if (errNo != Errno.SUCCESS.code) {
            throw SqliteException(errNo, errNo, "sqlite3__wasm_config_j() failed")
        }
    }

    fun sqlite3SetLogger(logger: SqliteLogCallback?) {
        val oldLogger = callbackStore.sqlite3LogCallback
        try {
            callbackStore.sqlite3LogCallback = logger
            val errNo = sqliteBindings.sqlite3__wasm_config_ii.executeForInt(
                SqliteConfigParameter.SQLITE_CONFIG_LOG.id,
                if (logger != null) callbackFunctionIndexes.loggingCallbackFunction.funcId else 0,
                0,
            )
            if (errNo != Errno.SUCCESS.code) {
                throw SqliteException(errNo, errNo, "sqlite3__wasm_config_ii() failed")
            }
        } catch (@Suppress("TooGenericExceptionCaught") ex: Throwable) {
            callbackStore.sqlite3LogCallback = oldLogger
            throw ex
        }
    }

    fun sqlite3SoftHeapLimit(limit: Long) {
        val errNo = sqliteBindings.sqlite3_soft_heap_limit64.executeForInt(limit)
        if (errNo != Errno.SUCCESS.code) {
            throw SqliteException(errNo, errNo, "sqlite3_soft_heap_limit64() failed")
        }
    }

    fun sqlite3ErrMsg(
        sqliteDb: WasmPtr<SqliteDb>,
    ): String? {
        val errorAddr: WasmPtr<Byte> = sqliteBindings.sqlite3_errmsg.executeForPtr(sqliteDb.addr)
        return memory.readNullableZeroTerminatedString(errorAddr)
    }

    fun sqlite3ErrCode(
        sqliteDb: WasmPtr<SqliteDb>,
    ): Int {
        return sqliteBindings.sqlite3_errcode.executeForInt(sqliteDb.addr)
    }

    fun sqlite3ExtendedErrCode(
        sqliteDb: WasmPtr<SqliteDb>,
    ): Int {
        return sqliteBindings.sqlite3_extended_errcode.executeForInt(sqliteDb.addr)
    }

    fun sqlite3CreateCollation(
        database: WasmPtr<SqliteDb>,
        name: String,
        comparator: SqliteComparatorCallback?,
    ) {
        val pCallbackId: SqliteComparatorId? = if (comparator != null) {
            callbackStore.sqlite3Comparators.put(comparator)
        } else {
            null
        }

        val pName: WasmPtr<Byte> = allocZeroTerminatedString(name)

        val errNo = sqliteBindings.sqlite3_create_collation_v2.executeForInt(
            database.addr,
            pName.addr,
            SqliteTextEncoding.SQLITE_UTF8.id,
            pCallbackId?.id,
            if (pCallbackId != null) callbackFunctionIndexes.comparatorFunction.funcId else 0,
            if (pCallbackId != null) callbackFunctionIndexes.destroyComparatorFunction.funcId else 0,
        )
        memoryBindings.sqliteFreeSilent(pName)
        if (errNo != Errno.SUCCESS.code && pCallbackId != null) {
            callbackStore.sqlite3Comparators.remove(pCallbackId)
        }
        errNo.throwOnSqliteError("sqlite3CreateCollation() failed", database)
    }

    fun sqlite3Exec(
        database: WasmPtr<SqliteDb>,
        sql: String,
        callback: SqliteExecCallback? = null,
    ) {
        var pSql: WasmPtr<Byte> = WasmPtr.sqlite3Null()
        var pzErrMsg: WasmPtr<WasmPtr<Byte>> = WasmPtr.sqlite3Null()
        val pCallbackId: SqliteExecCallbackId? = if (callback != null) {
            callbackStore.sqlite3ExecCallbacks.put(callback)
        } else {
            null
        }

        try {
            pSql = allocZeroTerminatedString(sql)
            pzErrMsg = memoryBindings.sqliteAllocOrThrow(WasmPtr.WASM_SIZEOF_PTR)

            val errNo = sqliteBindings.sqlite3_exec.executeForInt(
                database.addr,
                pSql.addr,
                if (pCallbackId != null) callbackFunctionIndexes.execCallbackFunction.funcId else 0,
                pCallbackId?.id ?: 0,
                pzErrMsg.addr,
            )
            if (errNo != Errno.SUCCESS.code) {
                val errMsgAddr: WasmPtr<Byte> = memory.readPtr(pzErrMsg)
                val errMsg = memory.readZeroTerminatedString(errMsgAddr)
                memoryBindings.sqliteFreeSilent(errMsgAddr)
                throw SqliteException(errNo, errNo, "sqlite3_exec", errMsg)
            }
        } finally {
            pCallbackId?.let { callbackStore.sqlite3ExecCallbacks.remove(it) }
            memoryBindings.sqliteFreeSilent(pSql)
            memoryBindings.sqliteFreeSilent(pzErrMsg)
        }
    }

    fun sqlite3DbReadonly(
        sqliteDb: WasmPtr<SqliteDb>,
        dbName: String?,
    ): SqliteDbReadonlyResult {
        val pDbName = if (dbName != null) {
            allocZeroTerminatedString(dbName)
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
    ) {
        val result = sqliteBindings.sqlite3_busy_timeout.executeForInt(sqliteDb.addr, ms)
        result.throwOnSqliteError("sqlite3BusyTimeout() failed", sqliteDb)
    }

    fun sqlite3Trace(
        sqliteDb: WasmPtr<SqliteDb>,
        mask: SqliteTraceEventCode,
        traceCallback: SqliteTraceCallback?,
    ) {
        if (traceCallback != null) {
            callbackStore.sqlite3TraceCallbacks[sqliteDb] = traceCallback
        }

        val errNo = sqliteBindings.sqlite3_trace_v2.executeForInt(
            sqliteDb.addr,
            mask.mask.toInt(),
            if (traceCallback != null) callbackFunctionIndexes.traceFunction.funcId else 0,
            sqliteDb.addr,
        )

        if (traceCallback == null || errNo != Errno.SUCCESS.code) {
            callbackStore.sqlite3TraceCallbacks.remove(sqliteDb)
        }

        errNo.throwOnSqliteError("sqlite3_trace_v2() failed", sqliteDb)
    }

    fun sqlite3ProgressHandler(
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

        if (activeCallback != null) {
            callbackStore.sqlite3ProgressCallbacks[sqliteDb] = activeCallback
        }

        val errNo = sqliteBindings.sqlite3_progress_handler.executeForInt(
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

    fun sqlite3DbStatus(
        sqliteDb: WasmPtr<SqliteDb>,
        op: SqliteDbStatusParameter,
        resetFlag: Boolean,
    ): SqliteDbStatusResult {
        var pCur: WasmPtr<Int> = WasmPtr.sqlite3Null()
        var pHiwtr: WasmPtr<Int> = WasmPtr.sqlite3Null()

        try {
            pCur = memoryBindings.sqliteAllocOrThrow(4U)
            pHiwtr = memoryBindings.sqliteAllocOrThrow(4U)

            val errCode = sqliteBindings.sqlite3_db_status.executeForInt(
                sqliteDb.addr,
                op.id,
                pCur.addr,
                pHiwtr.addr,
                if (resetFlag) 1 else 0,
            )
            errCode.throwOnSqliteError(null, sqliteDb)
            return SqliteDbStatusResult(0, 0)
        } finally {
            memoryBindings.sqliteFreeSilent(pCur)
            memoryBindings.sqliteFreeSilent(pHiwtr)
        }
    }

    fun sqlite3DbConfig(
        sqliteDb: WasmPtr<SqliteDb>,
        op: SqliteDbConfigParameter,
        arg1: Int,
        pArg2: WasmPtr<*>,
    ) {
        val errCode = sqliteBindings.sqlite3__wasm_db_config_ip.executeForInt(
            sqliteDb.addr,
            op.id,
            arg1,
            pArg2.addr,
        )
        errCode.throwOnSqliteError("sqlite3DbConfig() failed", sqliteDb)
    }

    fun sqlite3DbConfig(
        sqliteDb: WasmPtr<SqliteDb>,
        op: SqliteDbConfigParameter,
        pArg1: WasmPtr<*>,
        arg2: Int,
        arg3: Int,
    ) {
        val errCode = sqliteBindings.sqlite3__wasm_db_config_pii.executeForInt(
            sqliteDb.addr,
            op.id,
            pArg1.addr,
            arg2,
            arg3,
        )
        errCode.throwOnSqliteError("sqlite3DbConfig() failed", sqliteDb)
    }

    fun sqlite3DbConfig(sqliteDb: WasmPtr<SqliteDb>, op: SqliteDbConfigParameter, pArg1: WasmPtr<Byte>) {
        val errCode = sqliteBindings.sqlite3__wasm_db_config_s.executeForInt(
            sqliteDb.addr,
            op.id,
            pArg1.addr,
        )
        errCode.throwOnSqliteError("sqlite3DbConfig() failed", sqliteDb)
    }

    fun sqlite3ColumnCount(
        statement: WasmPtr<SqliteStatement>,
    ): Int {
        return sqliteBindings.sqlite3_column_count.executeForInt(statement.addr)
    }

    fun sqlite3ColumnText(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): String? {
        val ptr: WasmPtr<Byte> = sqliteBindings.sqlite3_column_text.executeForPtr(statement.addr, columnIndex)
        return memory.readNullableZeroTerminatedString(ptr)
    }

    fun sqlite3ColumnInt64(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): Long {
        return sqliteBindings.sqlite3_column_int64.executeForLong(statement.addr, columnIndex)
    }

    fun sqlite3ColumnDouble(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): Double {
        return sqliteBindings.sqlite3_column_double.executeForDouble(statement.addr, columnIndex)
    }

    fun sqlite3ColumnBlob(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): ByteArray {
        val ptr: WasmPtr<Byte> = sqliteBindings.sqlite3_column_text.executeForPtr(
            statement.addr,
            columnIndex,
        )
        val bytes = sqliteBindings.sqlite3_column_bytes.executeForInt(
            statement.addr,
            columnIndex,
        )
        return memory.readBytes(ptr, bytes)
    }

    fun registerAndroidFunctions(db: WasmPtr<SqliteDb>, utf16Storage: Boolean) {
        sqliteBindings.register_android_functions.executeForInt(
            db.addr,
            if (utf16Storage) 1 else 0,
        ).throwOnSqliteError("register_android_functions() failed", db)
    }

    fun registerLocalizedCollators(ptr: WasmPtr<SqliteDb>, newLocale: String, utf16Storage: Boolean) {
        var pNewLocale: WasmPtr<Byte> = WasmPtr.sqlite3Null()
        try {
            pNewLocale = allocZeroTerminatedString(newLocale)
            val errCode = sqliteBindings.register_localized_collators.executeForInt(
                ptr.addr,
                pNewLocale.addr,
                if (utf16Storage) 1 else 0,
            )
            errCode.throwOnSqliteError("register_localized_collators($newLocale) failed", ptr)
        } finally {
            memoryBindings.sqliteFreeSilent(pNewLocale)
        }
    }

    fun sqlite3Step(
        statement: WasmPtr<SqliteStatement>,
    ): SqliteErrno {
        val errCode = sqliteBindings.sqlite3_step.executeForInt(statement.addr)
        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    fun sqlite3Reset(
        statement: WasmPtr<SqliteStatement>,
    ): SqliteErrno {
        val errCode = sqliteBindings.sqlite3_reset.executeForInt(statement.addr)
        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    fun sqlite3ColumnType(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): SqliteColumnType {
        val type = sqliteBindings.sqlite3_column_type.executeForInt(statement.addr, columnIndex)
        return SqliteColumnType(type)
    }

    private fun Int.throwOnSqliteError(
        msgPrefix: String?,
        sqliteDb: WasmPtr<SqliteDb>,
    ) {
        if (this != Errno.SUCCESS.code) {
            val errInfo = readSqliteErrorInfo(sqliteDb)
            throw SqliteException(errInfo, msgPrefix)
        }
    }

    fun readSqliteErrorInfo(
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

    fun sqlite3Changes(sqliteDb: WasmPtr<SqliteDb>): Int {
        return sqliteBindings.sqlite3_changes.executeForInt(sqliteDb.addr)
    }

    fun sqlite3LastInsertRowId(
        sqliteDb: WasmPtr<SqliteDb>,
    ): Long {
        return sqliteBindings.sqlite3_last_insert_rowid.executeForLong(sqliteDb.addr)
    }

    fun sqlite3ClearBindings(statement: WasmPtr<SqliteStatement>): SqliteErrno {
        val errCode = sqliteBindings.sqlite3_clear_bindings.executeForInt(statement.addr)
        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    fun sqlite3BindBlobTransient(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
        value: ByteArray,
    ): SqliteErrno {
        val pValue: WasmPtr<Byte> = memoryBindings.sqliteAllocOrThrow(value.size.toUInt())
        memory.write(pValue, value, 0, value.size)
        val errCode = try {
            sqliteBindings.sqlite3_bind_blob.executeForInt(
                sqliteDb.addr,
                index,
                pValue.addr,
                value.size,
                SqliteDestructorType.SQLITE_TRANSIENT.id, // TODO: change to destructor?
            )
        } finally {
            memoryBindings.sqliteFreeSilent(pValue)
        }

        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    fun sqlite3BindStringTransient(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
        value: String,
    ): SqliteErrno {
        val encoded = value.encodeToByteArray()
        val size = encoded.size

        val pValue: WasmPtr<Byte> = memoryBindings.sqliteAllocOrThrow(size.toUInt())
        memory.write(pValue, encoded, 0, size)
        val errCode = try {
            sqliteBindings.sqlite3_bind_text.executeForInt(
                sqliteDb.addr,
                index,
                pValue.addr,
                size,
                SqliteDestructorType.SQLITE_TRANSIENT.id, // TODO: change to destructor?
            )
        } finally {
            memoryBindings.sqliteFreeSilent(pValue)
        }

        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    fun sqlite3BindDouble(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
        value: Double,
    ): SqliteErrno {
        val errCode = sqliteBindings.sqlite3_bind_double.executeForInt(
            sqliteDb.addr,
            index,
            value,
        )
        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    fun sqlite3BindLong(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
        value: Long,
    ): SqliteErrno {
        val errCode = sqliteBindings.sqlite3_bind_int64.executeForInt(
            sqliteDb.addr,
            index,
            value,
        )
        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    fun sqlite3BindNull(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
    ): SqliteErrno {
        val errCode = sqliteBindings.sqlite3_bind_int64.executeForInt(sqliteDb.addr, index)
        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    fun sqlite3ColumnName(
        statement: WasmPtr<SqliteStatement>,
        index: Int,
    ): String? {
        val ptr: WasmPtr<Byte> = sqliteBindings.sqlite3_column_name.executeForPtr(statement.addr, index)
        return memory.readNullableZeroTerminatedString(ptr)
    }

    fun sqlite3StmtReadonly(statement: WasmPtr<SqliteStatement>): Boolean {
        return sqliteBindings.sqlite3_stmt_readonly.executeForInt(statement.addr) != 0
    }

    fun sqlite3BindParameterCount(statement: WasmPtr<SqliteStatement>): Int {
        return sqliteBindings.sqlite3_bind_parameter_count.executeForInt(statement.addr)
    }

    fun sqlite3PrepareV2(
        sqliteDb: WasmPtr<SqliteDb>,
        sql: String,
    ): WasmPtr<SqliteStatement> {
        var sqlBytesPtr: WasmPtr<Byte> = WasmPtr.sqlite3Null()
        var ppStatement: WasmPtr<WasmPtr<SqliteStatement>> = WasmPtr.sqlite3Null()

        try {
            val sqlEncoded = sql.encodeToByteArray()
            val nullTerminatedSqlSize = sqlEncoded.size + 1

            sqlBytesPtr = memoryBindings.sqliteAllocOrThrow(nullTerminatedSqlSize.toUInt())
            ppStatement = memoryBindings.sqliteAllocOrThrow(WasmPtr.WASM_SIZEOF_PTR)

            memory.write(sqlBytesPtr, sqlEncoded)
            memory.writeByte(sqlBytesPtr + sqlEncoded.size, 0)

            val result = sqliteBindings.sqlite3_prepare_v2.executeForInt(
                sqliteDb.addr,
                sqlBytesPtr.addr,
                nullTerminatedSqlSize,
                ppStatement.addr,
                WasmPtr.sqlite3Null<Unit>().addr,
            )
            result.throwOnSqliteError("sqlite3_prepare_v2() failed", sqliteDb)
            return memory.readPtr(ppStatement).also {
                databaseResources.registerStatement(sqliteDb, it)
            }
        } finally {
            memoryBindings.sqliteFreeSilent(sqlBytesPtr)
            memoryBindings.sqliteFreeSilent(ppStatement)
        }
    }

    fun sqlite3Finalize(
        sqliteDatabase: WasmPtr<SqliteDb>,
        statement: WasmPtr<SqliteStatement>,
    ) {
        try {
            val errCode = sqliteBindings.sqlite3_finalize.executeForInt(statement.addr)
            errCode.throwOnSqliteError("sqlite3_finalize() failed", sqliteDatabase)
        } finally {
            databaseResources.unregisterStatement(sqliteDatabase, statement)
        }
    }

    fun sqlite3ExpandedSql(statement: WasmPtr<SqliteStatement>): String? {
        val ptr: WasmPtr<Byte> = sqliteBindings.sqlite3_expanded_sql.executeForPtr(statement.addr)
        return memory.readNullableZeroTerminatedString(ptr)
    }

    private fun allocZeroTerminatedString(string: String): WasmPtr<Byte> {
        val bytes = string.encodeToByteArray()
        val mem: WasmPtr<Byte> = memoryBindings.sqliteAllocOrThrow(bytes.size.toUInt() + 1U)
        memory.writeZeroTerminatedString(mem, string)
        return mem
    }

    data class Sqlite3Version(
        val version: String,
        val versionNumber: Int,
        val sourceId: String,
    )

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
