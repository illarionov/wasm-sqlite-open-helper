/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.interop.capi

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.isSqlite3Null
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.EmbedderMemory
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readNullableZeroTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrorInfo
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode

/**
 * Wrappers for SQLite3 C Api error functions: `sqlite3_errcode`, `sqlite3_extended_errcode`, `sqlite3_errmsg`
 */
internal class Sqlite3ErrorFunctions(
    private val sqliteBindings: SqliteBindings,
    private val memory: EmbedderMemory,
) {
    fun sqlite3ErrCode(
        sqliteDb: WasmPtr<SqliteDb>,
    ): SqliteResultCode {
        return sqliteBindings.sqlite3_errcode.executeForSqliteResultCode(sqliteDb.addr)
    }

    fun sqlite3ExtendedErrCode(
        sqliteDb: WasmPtr<SqliteDb>,
    ): SqliteResultCode {
        return sqliteBindings.sqlite3_extended_errcode.executeForSqliteResultCode(sqliteDb.addr)
    }

    fun sqlite3ErrMsg(
        sqliteDb: WasmPtr<SqliteDb>,
    ): String? {
        val errorAddr: WasmPtr<Byte> = sqliteBindings.sqlite3_errmsg.executeForPtr(sqliteDb.addr)
        return memory.readNullableZeroTerminatedString(errorAddr)
    }

    companion object {
        fun Sqlite3ErrorFunctions.readSqliteErrorInfo(
            sqliteDb: WasmPtr<SqliteDb>,
        ): SqliteErrorInfo {
            if (sqliteDb.isSqlite3Null()) {
                return SqliteErrorInfo(SqliteResultCode.SQLITE_OK, SqliteResultCode.SQLITE_OK, null)
            }

            val errCode = sqlite3ErrCode(sqliteDb)
            val extendedErrCode = sqlite3ExtendedErrCode(sqliteDb)
            val errMsg = if (errCode != SqliteResultCode.SQLITE_OK) {
                sqlite3ErrMsg(sqliteDb)
            } else {
                null
            }
            return SqliteErrorInfo(errCode, extendedErrCode, errMsg)
        }
    }
}
