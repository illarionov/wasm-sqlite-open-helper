/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.open

import ru.pixnews.wasm.sqlite.open.helper.common.api.SqliteUintBitMask
import kotlin.jvm.JvmInline

@JvmInline
public value class OpenFileFlags(
    public override val mask: UInt,
) : SqliteUintBitMask<OpenFileFlags> {
    override val newInstance: (UInt) -> OpenFileFlags get() = ::OpenFileFlags

    override fun toString(): String {
        return "OpenFileFlags(0x${mask.toString(16)})"
    }

    @Suppress("BLANK_LINE_BETWEEN_PROPERTIES")
    public companion object OpenFileFlag {
        public const val O_RDONLY: UInt = 0x0U
        public const val O_WRONLY: UInt = 0x1U
        public const val O_RDWR: UInt = 0x2U

        public const val O_CREAT: UInt = 0x40U
        public const val O_EXCL: UInt = 0x80U
        public const val O_NOCTTY: UInt = 0x100U
        public const val O_TRUNC: UInt = 0x200U
        public const val O_APPEND: UInt = 0x400U
        public const val O_NONBLOCK: UInt = 0x800U
        public const val O_NDELAY: UInt = O_NONBLOCK
        public const val O_DSYNC: UInt = 0x1000U
        public const val O_ASYNC: UInt = 0x2000U
        public const val O_DIRECT: UInt = 0x4000U
        public const val O_LARGEFILE: UInt = 0x8000U
        public const val O_DIRECTORY: UInt = 0x10000U
        public const val O_NOFOLLOW: UInt = 0x20000U
        public const val O_NOATIME: UInt = 0x40000U
        public const val O_CLOEXEC: UInt = 0x80000U
        public const val O_SYNC: UInt = 0x101000U
        public const val O_PATH: UInt = 0x200000U
        public const val O_TMPFILE: UInt = 0x410000U
        public const val O_SEARCH: UInt = O_PATH
    }
}
