/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.include

import ru.pixnews.wasm.sqlite.open.helper.common.api.SqliteUintBitMask
import kotlin.jvm.JvmInline

/**
 * File mode bits (mode_t)
 */
@JvmInline
public value class FileMode(
    public override val mask: UInt,
) : SqliteUintBitMask<FileMode> {
    override val newInstance: (UInt) -> FileMode get() = ::FileMode
    override fun toString(): String = fileModeToString()
}

public fun FileMode.fileModeToString(): String = "0${mask.toString(8)}"

public fun FileMode.fileModeToStringLong(): String = maskToString(
    mask,
    listOf(
        Fcntl::S_ISUID,
        Fcntl::S_ISGID,
        Fcntl::S_ISVTX,
        Fcntl::S_IRUSR,
        Fcntl::S_IWUSR,
        Fcntl::S_IXUSR,
        Fcntl::S_IRWXU,
        Fcntl::S_IRGRP,
        Fcntl::S_IWGRP,
        Fcntl::S_IXGRP,
        Fcntl::S_IRWXG,
        Fcntl::S_IROTH,
        Fcntl::S_IWOTH,
        Fcntl::S_IXOTH,
        Fcntl::S_IRWXO,
    ),
)
