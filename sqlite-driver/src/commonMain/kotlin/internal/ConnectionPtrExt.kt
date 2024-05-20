/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.internal

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.exception.throwAndroidSqliteException
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3CApi
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3ErrorFunctions.Companion.readSqliteErrorInfo
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3Result

internal fun <R : Any> Sqlite3Result<R>.getOrThrow(
    msgPrefix: String?,
): R = when (this) {
    is Sqlite3Result.Success<R> -> this.value
    is Sqlite3Result.Error -> throwAndroidSqliteException(this.info, msgPrefix)
}

internal fun Sqlite3CApi.readErrorThrowAndroidSqliteException(
    connection: WasmPtr<SqliteDb>,
    message: String? = null,
): Nothing {
    val errInfo = errors.readSqliteErrorInfo(connection)
    throwAndroidSqliteException(errInfo, message)
}

internal fun SqliteResultCode.throwOnError(
    msgPrefix: String?,
) {
    if (this != SqliteResultCode.SQLITE_OK) {
        throwAndroidSqliteException(msgPrefix, this)
    }
}
