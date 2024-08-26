/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.linux

import arrow.core.Either
import platform.posix.EACCES
import platform.posix.EBADF
import platform.posix.EINVAL
import platform.posix.ELOOP
import platform.posix.ENAMETOOLONG
import platform.posix.ENOENT
import platform.posix.ENOMEM
import platform.posix.ENOTDIR
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.AccessDenied
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NameTooLong
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NoEntry
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NotDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.StatError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.TooManySymbolicLinks
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.delegatefs.FileSystemOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.linux.ext.toDirFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.Stat
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.AT_SYMLINK_NOFOLLOW

internal expect fun platformFstatat(
    dirfd: Int,
    path: String,
    statFlags: Int,
): Either<Int, StructStat>

internal object LinuxStat : FileSystemOperationHandler<Stat, StatError, StructStat> {
    override fun invoke(input: Stat): Either<StatError, StructStat> {
        return platformFstatat(
            input.baseDirectory.toDirFd(),
            input.path,
            input.getStatFlags(),
        ).mapLeft {
            it.errnoToStatError(input)
        }
    }

    private fun Stat.getStatFlags(): Int = if (this.followSymlinks) {
        0
    } else {
        AT_SYMLINK_NOFOLLOW
    }

    private fun Int.errnoToStatError(request: Stat): StatError = when (this) {
        EACCES -> AccessDenied("Access denied for `$request`")
        EBADF -> BadFileDescriptor("Bad file descriptor ${request.baseDirectory}")
        EINVAL -> InvalidArgument("Invalid argument in `$request`")
        ELOOP -> TooManySymbolicLinks("Too many symlinks while resolving `$request`")
        ENAMETOOLONG -> NameTooLong("Name too long while resolving `$request`")
        ENOENT -> NoEntry("Component of `${request.path}` does not exist")
        ENOMEM -> IoError("No memory")
        ENOTDIR -> NotDirectory("Error while resolving `${request.path}`: not a directory")
        else -> InvalidArgument("Error `$this`")
    }
}
