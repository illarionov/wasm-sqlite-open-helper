/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.interop

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.EmbedderMemory
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readNullableZeroTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readZeroTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.allocZeroTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.sqliteFreeSilent
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore.SqliteComparatorId
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore.SqliteExecCallbackId
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteComparatorCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteException
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteExecCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTextEncoding

// Unused methods from Sqlite C Api
internal class SqliteApiCompanion(
    private val sqliteBindings: SqliteBindings,
    private val memory: EmbedderMemory,
    private val callbackStore: JvmSqliteCallbackStore,
    private val callbackFunctionIndexes: Sqlite3CallbackFunctionIndexes,
) {
    private val memoryBindings = sqliteBindings.memoryBindings

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

    fun sqlite3CreateCollation(
        database: WasmPtr<SqliteDb>,
        name: String,
        comparator: SqliteComparatorCallback?,
    ): Int {
        val pCallbackId: SqliteComparatorId? = if (comparator != null) {
            callbackStore.sqlite3Comparators.put(comparator)
        } else {
            null
        }

        val pName: WasmPtr<Byte> = sqliteBindings.memoryBindings.allocZeroTerminatedString(memory, name)

        val errNo = sqliteBindings.sqlite3_create_collation_v2.executeForInt(
            database.addr,
            pName.addr,
            SqliteTextEncoding.SQLITE_UTF8.id,
            pCallbackId?.id,
            if (pCallbackId != null) callbackFunctionIndexes.comparatorFunction.funcId else 0,
            if (pCallbackId != null) callbackFunctionIndexes.destroyComparatorFunction.funcId else 0,
        )
        sqliteBindings.memoryBindings.sqliteFreeSilent(pName)
        if (errNo != Errno.SUCCESS.code && pCallbackId != null) {
            callbackStore.sqlite3Comparators.remove(pCallbackId)
        }
        return errNo
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
            pSql = memoryBindings.allocZeroTerminatedString(memory, sql)
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

    data class Sqlite3Version(
        val version: String,
        val versionNumber: Int,
        val sourceId: String,
    )
}
