/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.sqlite.common.capi

import at.released.wasm.sqlite.open.helper.WasmPtr
import at.released.wasm.sqlite.open.helper.embedder.exports.SqliteExports
import at.released.wasm.sqlite.open.helper.isNull
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrorInfo
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode.Companion.SQLITE_OK
import at.released.weh.wasm.core.memory.ReadOnlyMemory
import at.released.weh.wasm.core.memory.readNullableNullTerminatedString

/**
 * Wrappers for SQLite3 C Api error functions: `sqlite3_errcode`, `sqlite3_extended_errcode`, `sqlite3_errmsg`
 */
public class Sqlite3ErrorFunctions internal constructor(
    private val sqliteExports: SqliteExports,
    private val memory: ReadOnlyMemory,
) {
    public fun sqlite3ErrCode(
        sqliteDb: WasmPtr<SqliteDb>,
    ): SqliteResultCode {
        return sqliteExports.sqlite3_errcode.executeForSqliteResultCode(sqliteDb.addr)
    }

    public fun sqlite3ExtendedErrCode(
        sqliteDb: WasmPtr<SqliteDb>,
    ): SqliteResultCode {
        return sqliteExports.sqlite3_extended_errcode.executeForSqliteResultCode(sqliteDb.addr)
    }

    public fun sqlite3ErrMsg(
        sqliteDb: WasmPtr<SqliteDb>,
    ): String? {
        val errorAddr: WasmPtr<Byte> = WasmPtr(sqliteExports.sqlite3_errmsg.executeForPtr(sqliteDb.addr))
        return memory.readNullableNullTerminatedString(errorAddr.addr)
    }

    public companion object {
        public fun <R : Any> Sqlite3ErrorFunctions.createSqlite3Result(
            errCode: SqliteResultCode,
            result: R,
            sqliteDb: WasmPtr<SqliteDb>? = null,
        ): Sqlite3Result<R> = if (errCode == SQLITE_OK) {
            Sqlite3Result.Success(result)
        } else {
            val errorInfo = if (sqliteDb != null) {
                this.readSqliteErrorInfo(sqliteDb)
            } else {
                SqliteErrorInfo(errCode, errCode, null)
            }
            Sqlite3Result.Error(errorInfo)
        }

        public fun Sqlite3ErrorFunctions.readSqliteErrorInfo(
            sqliteDb: WasmPtr<SqliteDb>,
        ): SqliteErrorInfo {
            if (sqliteDb.isNull()) {
                return SqliteErrorInfo(SQLITE_OK, SQLITE_OK, null)
            }

            val errCode = sqlite3ErrCode(sqliteDb)
            val extendedErrCode = sqlite3ExtendedErrCode(sqliteDb)
            val errMsg = if (errCode != SQLITE_OK) {
                sqlite3ErrMsg(sqliteDb)
            } else {
                null
            }
            return SqliteErrorInfo(errCode, extendedErrCode, errMsg)
        }
    }
}
