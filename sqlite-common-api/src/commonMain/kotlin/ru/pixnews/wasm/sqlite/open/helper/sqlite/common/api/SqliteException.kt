/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api

import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode.Companion.name

public class SqliteException(
    public val errorInfo: SqliteErrorInfo,
    prefix: String? = null,
) : RuntimeException(
    errorInfo.formatErrorMessage(prefix),
) {
    public constructor(
        sqliteErrorCode: SqliteResultCode,
        sqliteExtendedErrorCode: SqliteResultCode,
        prefix: String? = null,
        sqliteMsg: String? = null,
    ) : this(SqliteErrorInfo(sqliteErrorCode, sqliteExtendedErrorCode, sqliteMsg), prefix)

    public companion object {
        public val SqliteException.sqlite3ErrNoName: String get() = errorInfo.sqliteExtendedErrorCode.name
    }
}
