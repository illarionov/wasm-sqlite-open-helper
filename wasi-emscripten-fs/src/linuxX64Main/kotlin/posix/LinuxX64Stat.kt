/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.StatError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.FileModeType
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.Stat
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.StructTimespec
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base.PosixOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.ext.toDirFd
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.AT_SYMLINK_NOFOLLOW
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.fstatat
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.stat
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.timespec

internal object LinuxX64Stat : PosixOperationHandler<Stat, StatError, StructStat> {
    override fun invoke(input: Stat): Either<StatError, StructStat> = memScoped {
        val statBuf: stat = alloc()
        val exitCode = fstatat(
            input.baseDirectory.toDirFd(),
            input.path,
            statBuf.ptr,
            input.getStatFlags(),
        )
        return if (exitCode == 0) {
            statBuf.toStructStat().right()
        } else {
            errno.errnoToStatError(input).left()
        }
    }

    private fun Stat.getStatFlags(): Int = if (this.followSymlinks) {
        0
    } else {
        AT_SYMLINK_NOFOLLOW
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

    private fun timespec.toStructTimespec(): StructTimespec = StructTimespec(
        seconds = this.tv_sec.toULong(),
        nanoseconds = this.tv_nsec.toULong(),
    )
}