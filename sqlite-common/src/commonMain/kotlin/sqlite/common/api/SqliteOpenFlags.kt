/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api

import ru.pixnews.wasm.sqlite.open.helper.common.api.SqliteUintBitMask

@JvmInline
public value class SqliteOpenFlags(
    override val mask: UInt,
) : SqliteUintBitMask<SqliteOpenFlags> {
    override val newInstance: (UInt) -> SqliteOpenFlags get() = ::SqliteOpenFlags

    public companion object {
        public val EMPTY: SqliteOpenFlags = SqliteOpenFlags(0U)

        /** Open flag to open in the database in read only mode  */
        public val SQLITE_OPEN_READONLY: SqliteOpenFlags = SqliteOpenFlags(0x0000_0001_U)

        /** Open flag to open in the database in read/write mode  */
        public val SQLITE_OPEN_READWRITE: SqliteOpenFlags = SqliteOpenFlags(0x0000_0002_U)

        /** Open flag to create the database if it does not exist  */
        public val SQLITE_OPEN_CREATE: SqliteOpenFlags = SqliteOpenFlags(0x0000_0004_U)

        /** VFS only */
        public val SQLITE_OPEN_DELETEONCLOSE: SqliteOpenFlags = SqliteOpenFlags(0x0000_0008_U)

        /** VFS only */
        public val SQLITE_OPEN_EXCLUSIVE: SqliteOpenFlags = SqliteOpenFlags(0x0000_0010_U)

        /** VFS only */
        public val SQLITE_OPEN_AUTOPROXY: SqliteOpenFlags = SqliteOpenFlags(0x0000_0020_U)

        /** Open flag to support URI filenames  */
        public val SQLITE_OPEN_URI: SqliteOpenFlags = SqliteOpenFlags(0x0000_0040_U)

        /** VFS only */
        public val SQLITE_OPEN_MEMORY: SqliteOpenFlags = SqliteOpenFlags(0x0000_0080_U)

        /** VFS only */
        public val SQLITE_OPEN_MAIN_DB: SqliteOpenFlags = SqliteOpenFlags(0x0000_0100_U)

        /** VFS only */
        public val SQLITE_OPEN_TEMP_DB: SqliteOpenFlags = SqliteOpenFlags(0x0000_0200_U)

        /** VFS only */
        public val SQLITE_OPEN_TRANSIENT_DB: SqliteOpenFlags = SqliteOpenFlags(0x0000_0400_U)

        /** VFS only */
        public val SQLITE_OPEN_MAIN_JOURNAL: SqliteOpenFlags = SqliteOpenFlags(0x0000_0800_U)

        /** VFS only */
        public val SQLITE_OPEN_TEMP_JOURNAL: SqliteOpenFlags = SqliteOpenFlags(0x0000_1000_U)

        /** VFS only */
        public val SQLITE_OPEN_SUBJOURNAL: SqliteOpenFlags = SqliteOpenFlags(0x0000_2000_U)

        /** VFS only */
        public val SQLITE_OPEN_SUPER_JOURNAL: SqliteOpenFlags = SqliteOpenFlags(0x0000_4000_U)

        /** Open flag opens the database in multi-thread threading mode  */
        public val SQLITE_OPEN_NOMUTEX: SqliteOpenFlags = SqliteOpenFlags(0x0000_8000_U)

        /** Open flag opens the database in serialized threading mode  */
        public val SQLITE_OPEN_FULLMUTEX: SqliteOpenFlags = SqliteOpenFlags(0x0001_0000_U)

        /** Open flag opens the database in shared cache mode  */
        public val SQLITE_OPEN_SHAREDCACHE: SqliteOpenFlags = SqliteOpenFlags(0x0002_0000_U)

        /** Open flag opens the database in private cache mode  */
        public val SQLITE_OPEN_PRIVATECACHE: SqliteOpenFlags = SqliteOpenFlags(0x0004_0000_U)

        /** VFS only */
        public val SQLITE_OPEN_WAL: SqliteOpenFlags = SqliteOpenFlags(0x0008_0000_U)

        /** VFS only */
        public val SQLITE_OPEN_NOFOLLOW: SqliteOpenFlags = SqliteOpenFlags(0x0100_0000_U)

        /** VFS only */
        public val SQLITE_OPEN_EXRESCODE: SqliteOpenFlags = SqliteOpenFlags(0x0200_0000_U)
    }
}
