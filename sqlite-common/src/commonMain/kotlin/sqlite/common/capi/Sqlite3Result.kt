/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi

import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrorInfo
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode
import kotlin.jvm.JvmInline

@Suppress("WRONG_ORDER_IN_CLASS_LIKE_STRUCTURES")
public sealed interface Sqlite3Result<out R : Any> {
    public val sqliteErrCode: SqliteResultCode

    @JvmInline
    public value class Success<out R : Any>(
        public val value: R,
    ) : Sqlite3Result<R> {
        override val sqliteErrCode: SqliteResultCode get() = SqliteResultCode.SQLITE_OK
    }

    @JvmInline
    public value class Error(
        public val info: SqliteErrorInfo,
    ) : Sqlite3Result<Nothing> {
        override val sqliteErrCode: SqliteResultCode get() = info.sqliteExtendedErrorCode
    }
}
