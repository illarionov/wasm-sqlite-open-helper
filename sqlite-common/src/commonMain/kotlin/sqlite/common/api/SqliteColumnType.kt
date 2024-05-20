/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api

import kotlin.jvm.JvmInline

/**
 * SQLite Fundamental Datatypes
 *
 * https://www.sqlite.org/c3ref/c_blob.html
 */
@JvmInline
public value class SqliteColumnType(
    public val id: Int,
) {
    public companion object {
        public val SQLITE_INTEGER: SqliteColumnType = SqliteColumnType(1)
        public val SQLITE_FLOAT: SqliteColumnType = SqliteColumnType(2)
        public val SQLITE_BLOB: SqliteColumnType = SqliteColumnType(4)
        public val SQLITE_NULL: SqliteColumnType = SqliteColumnType(5)
        public val SQLITE3_TEXT: SqliteColumnType = SqliteColumnType(3)
    }
}
