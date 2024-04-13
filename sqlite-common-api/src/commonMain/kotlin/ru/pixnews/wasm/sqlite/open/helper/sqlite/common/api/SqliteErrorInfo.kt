/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi

public data class SqliteErrorInfo(
    val sqliteErrorCode: Int,
    val sqliteExtendedErrorCode: Int = sqliteErrorCode,
    val sqliteMsg: String? = null,
)

@InternalWasmSqliteHelperApi
public fun SqliteErrorInfo.formatErrorMessage(prefix: String?): String = buildString {
    append("Sqlite error ")
    append(SqliteException.sqlite3ErrNoName(sqliteErrorCode))
    append("/")
    append(SqliteException.sqlite3ErrNoName(sqliteExtendedErrorCode))
    if (prefix?.isNotEmpty() == true) {
        append(": ")
        append(prefix)
    }
    if (sqliteMsg != null) {
        append("; ")
        append(sqliteMsg)
    }
}
