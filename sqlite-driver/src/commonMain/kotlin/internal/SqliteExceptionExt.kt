/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.driver.internal

import androidx.sqlite.throwSQLiteException
import at.released.wasm.sqlite.open.helper.WasmPtr
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrorInfo
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode.Companion.SQLITE_OK
import at.released.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3CApi
import at.released.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3ErrorFunctions.Companion.readSqliteErrorInfo
import at.released.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3Result

internal fun <R : Any> Sqlite3Result<R>.getOrThrow(
    msgPrefix: String?,
): R = when (this) {
    is Sqlite3Result.Success<R> -> this.value
    is Sqlite3Result.Error -> throwSqliteException(this.info, msgPrefix)
}

internal fun Sqlite3CApi.readErrorThrowSqliteException(
    connection: WasmPtr<SqliteDb>,
    message: String? = null,
): Nothing {
    val errInfo = errors.readSqliteErrorInfo(connection)
    throwSqliteException(errInfo, message)
}

internal fun SqliteResultCode.throwOnError(
    msgPrefix: String?,
) {
    if (this != SqliteResultCode.SQLITE_OK) {
        throwSqliteException(msgPrefix, this)
    }
}

internal fun throwSqliteException(
    message: String?,
    errorCode: SqliteResultCode = SQLITE_OK,
): Nothing = throwSqliteException(
    SqliteErrorInfo(errorCode, errorCode, null),
    message,
)

@Suppress("CyclomaticComplexMethod")
internal fun throwSqliteException(
    errorInfo: SqliteErrorInfo,
    message: String?,
    cause: Throwable? = null,
): Nothing {
    val fullErMsg = if (errorInfo.sqliteMsg != null) {
        buildString {
            append("Extended code ", errorInfo.sqliteExtendedErrorCode)
            val msgs = listOfNotNull(message, errorInfo.sqliteMsg)
            if (msgs.isNotEmpty()) {
                msgs.joinTo(
                    buffer = this,
                    separator = ", ",
                    prefix = ": ",
                )
            }
            if (cause != null) {
                append("; ")
                append(cause.toString())
            }
        }
    } else {
        message
    }
    throwSQLiteException(errorInfo.sqliteErrorCode.id, fullErMsg)
}
