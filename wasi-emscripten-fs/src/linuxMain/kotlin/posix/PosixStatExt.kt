/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix

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
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.FileModeType
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.Stat
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.StatFd

internal fun FileModeType.Companion.fromLinuxModeType(
    linuxModeType: UInt,
): FileModeType {
    var type = linuxModeType.toInt() and platform.posix.S_IFMT
    val typeMask = when (type) {
        platform.posix.S_IFDIR -> FileModeType.S_IFDIR
        platform.posix.S_IFCHR -> FileModeType.S_IFCHR
        platform.posix.S_IFBLK -> FileModeType.S_IFBLK
        platform.posix.S_IFREG -> FileModeType.S_IFREG
        platform.posix.S_IFIFO -> FileModeType.S_IFIFO
        platform.posix.S_IFLNK -> FileModeType.S_IFLNK
        platform.posix.S_IFSOCK -> FileModeType.S_IFSOCK
        else -> error("Unexpected type 0x${type.toString(16)}")
    }

    val modeMask = linuxModeType and 0xfffU
    return FileModeType(typeMask or modeMask)
}

internal fun Int.errnoToStatError(request: Stat): StatError = when (this) {
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

internal fun Int.errnoToStatFdError(request: StatFd): StatError = when (this) {
    EACCES -> AccessDenied("Access denied for `$request`")
    EBADF -> BadFileDescriptor("Bad file descriptor ${request.fd}")
    ENOMEM -> IoError("No memory")
    else -> InvalidArgument("Error `$this`")
}
