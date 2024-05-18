/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper

import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion
import ru.pixnews.wasm.sqlite.open.helper.common.api.SqliteUintBitMask
import ru.pixnews.wasm.sqlite.open.helper.common.api.clear
import ru.pixnews.wasm.sqlite.open.helper.common.api.or
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteOpenFlags
import kotlin.jvm.JvmInline

@JvmInline
public value class OpenFlags(
    override val mask: UInt,
) : SqliteUintBitMask<OpenFlags> {
    override val newInstance: (UInt) -> OpenFlags get() = ::OpenFlags

    public companion object {
        public val EMPTY: OpenFlags = OpenFlags(0U)

        /** Open flag to open in the database in read only mode  */
        public val OPEN_READONLY: OpenFlags = OpenFlags(0x0000_0001_U)

        /** Open flag to open in the database in read/write mode  */
        public val OPEN_READWRITE: OpenFlags = OpenFlags(0x0000_0002_U)

        /** Open flag to create the database if it does not exist  */
        public val OPEN_CREATE: OpenFlags = OpenFlags(0x0000_0004_U)

        /**
         * Open flag: Flag for [openDatabase] to open the database without support for
         * localized collators.
         */
        public val NO_LOCALIZED_COLLATORS: OpenFlags = OpenFlags(0x0000_0010_U)

        /** Open flag to support URI filenames  */
        public val OPEN_URI: OpenFlags = OpenFlags(0x0000_0040_U)

        /** Open flag opens the database in multi-thread threading mode  */
        public val OPEN_NOMUTEX: OpenFlags = OpenFlags(0x0000_8000_U)

        /** Open flag opens the database in serialized threading mode  */
        public val OPEN_FULLMUTEX: OpenFlags = OpenFlags(0x0001_0000_U)

        /** Open flag opens the database in shared cache mode  */
        public val OPEN_SHAREDCACHE: OpenFlags = OpenFlags(0x0002_0000_U)

        /** Open flag opens the database in private cache mode  */
        public val OPEN_PRIVATECACHE: OpenFlags = OpenFlags(0x0004_0000_U)

        /** Open flag equivalent to [.OPEN_READWRITE] | [.OPEN_CREATE]  */
        public val CREATE_IF_NECESSARY: OpenFlags = OPEN_READWRITE or OPEN_CREATE

        /** Open flag to enable write-ahead logging  */
        // custom flag remove for sqlite3_open_v2
        public val ENABLE_WRITE_AHEAD_LOGGING: OpenFlags = OpenFlags(0x2000_0000_U)
    }
}

internal fun OpenFlags.toSqliteOpenFlags(): SqliteOpenFlags = SqliteOpenFlags(
    (this clear Companion.ENABLE_WRITE_AHEAD_LOGGING).mask,
)
