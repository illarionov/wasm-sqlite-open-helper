/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.sqlite.common.api

import at.released.wasm.sqlite.open.helper.InternalWasmSqliteHelperApi
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode.Companion.name

public data class SqliteErrorInfo(
    val sqliteErrorCode: SqliteResultCode,
    val sqliteExtendedErrorCode: SqliteResultCode = sqliteErrorCode,
    val sqliteMsg: String? = null,
)

@InternalWasmSqliteHelperApi
public fun SqliteErrorInfo.formatErrorMessage(prefix: String?): String = buildString {
    append("Sqlite error ")
    append(sqliteErrorCode.name)
    append("/")
    append(sqliteExtendedErrorCode.name)
    if (prefix?.isNotEmpty() == true) {
        append(": ")
        append(prefix)
    }
    if (sqliteMsg != null) {
        append("; ")
        append(sqliteMsg)
    }
}
