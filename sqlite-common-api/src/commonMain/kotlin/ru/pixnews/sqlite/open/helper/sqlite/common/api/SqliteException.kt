/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.sqlite.common.api

import ru.pixnews.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi

public class SqliteException(
    public val errorInfo: SqliteErrorInfo,
    prefix: String? = null,
) : RuntimeException(
    errorInfo.formatErrorMessage(prefix),
) {
    public constructor(
        sqliteErrorCode: Int,
        sqliteExtendedErrorCode: Int,
        prefix: String? = null,
        sqliteMsg: String? = null,
    ) : this(SqliteErrorInfo(sqliteErrorCode, sqliteExtendedErrorCode, sqliteMsg), prefix)

    public companion object {
        public val SqliteException.sqlite3ErrNoName: String get() = sqlite3ErrNoName(errorInfo.sqliteExtendedErrorCode)

        internal fun sqlite3ErrNoName(errNo: Int): String =
            SqliteErrno.fromErrNoCode(errNo)?.toString() ?: errNo.toString()
    }
}

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
