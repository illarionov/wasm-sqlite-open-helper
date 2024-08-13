/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.include

import ru.pixnews.wasm.sqlite.open.helper.common.api.SqliteUintBitMask
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysStat
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysStat.S_IFBLK
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysStat.S_IFCHR
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysStat.S_IFDIR
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysStat.S_IFIFO
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysStat.S_IFLNK
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysStat.S_IFREG
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysStat.S_IFSOCK
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Filetype
import kotlin.jvm.JvmInline

/**
 * File mode and file type bits
 */
@JvmInline
public value class FileModeType(
    public override val mask: UInt,
) : SqliteUintBitMask<FileModeType> {
    override val newInstance: (UInt) -> FileModeType get() = ::FileModeType
    public val mode: FileMode
        get() = FileMode(mask and 0xfffU)

    public val type: Filetype
        get() = when (mask and SysStat.S_IFMT) {
            S_IFDIR -> Filetype.DIRECTORY
            S_IFCHR -> Filetype.CHARACTER_DEVICE
            S_IFBLK -> Filetype.BLOCK_DEVICE
            S_IFREG -> Filetype.REGULAR_FILE
            S_IFIFO -> Filetype.UNKNOWN
            S_IFLNK -> Filetype.SYMBOLIC_LINK
            S_IFSOCK -> Filetype.SOCKET_STREAM // XXX
            else -> Filetype.UNKNOWN
        }

    override fun toString(): String = fileModeTypeToString(mask)
}
