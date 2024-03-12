/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.sqlite.common.api

import kotlin.jvm.JvmInline

/**
 * SQLite C Interface: Text Encodings
 *
 * https://www.sqlite.org/c3ref/c_any.html
 */
@JvmInline
public value class SqliteTextEncoding(
    public val id: Int,
) {
    public companion object {
        public val SQLITE_UTF8: SqliteTextEncoding = SqliteTextEncoding(1)
        public val SQLITE_UTF16LE: SqliteTextEncoding = SqliteTextEncoding(2)
        public val SQLITE_UTF16BE: SqliteTextEncoding = SqliteTextEncoding(3)
        public val SQLITE_UTF16: SqliteTextEncoding = SqliteTextEncoding(4)
        public val SQLITE_UTF16_ALIGNED: SqliteTextEncoding = SqliteTextEncoding(8)
        public val entriesMap: Map<Int, SqliteTextEncoding> = listOf(
            SQLITE_UTF8,
            SQLITE_UTF16LE,
            SQLITE_UTF16BE,
            SQLITE_UTF16,
            SQLITE_UTF16_ALIGNED,
        ).associateBy(SqliteTextEncoding::id)
    }
}
