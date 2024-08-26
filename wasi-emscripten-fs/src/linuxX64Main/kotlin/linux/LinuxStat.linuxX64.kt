/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.linux

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.errno
import platform.posix.stat
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.linux.ext.toStructTimespec
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.FileModeType
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.fstatat

internal actual fun platformFstatat(
    dirfd: Int,
    path: String,
    statFlags: Int,
): Either<Int, StructStat> = memScoped {
    val statBuf: stat = alloc()
    val exitCode = fstatat(
        dirfd,
        path,
        statBuf.ptr,
        statFlags,
    )
    return if (exitCode == 0) {
        statBuf.toStructStat().right()
    } else {
        errno.left()
    }
}

internal fun stat.toStructStat(): StructStat = StructStat(
    deviceId = st_dev,
    inode = st_ino,
    mode = FileModeType.fromLinuxModeType(st_mode),
    links = st_nlink,
    usedId = st_uid.toULong(),
    groupId = st_gid.toULong(),
    specialFileDeviceId = st_rdev,
    size = st_size.toULong(),
    blockSize = st_blksize.toULong(),
    blocks = st_blocks.toULong(),
    accessTime = st_atim.toStructTimespec(),
    modificationTime = st_mtim.toStructTimespec(),
    changeStatusTime = st_ctim.toStructTimespec(),
)
