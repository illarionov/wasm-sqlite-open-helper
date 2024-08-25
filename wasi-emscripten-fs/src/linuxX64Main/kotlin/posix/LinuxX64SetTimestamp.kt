/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import platform.posix.EACCES
import platform.posix.EBADF
import platform.posix.EINVAL
import platform.posix.EIO
import platform.posix.ELOOP
import platform.posix.ENAMETOOLONG
import platform.posix.ENOENT
import platform.posix.ENOTDIR
import platform.posix.EPERM
import platform.posix.EROFS
import platform.posix.ESRCH
import platform.posix.errno
import platform.posix.timespec
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.AccessDenied
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NameTooLong
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NoEntry
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NotDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.PermissionDenied
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.ReadOnlyFileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.SetTimestampError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.TooManySymbolicLinks
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.settimestamp.SetTimestamp
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base.PosixOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.ext.toDirFd
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.AT_SYMLINK_NOFOLLOW
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.UTIME_OMIT
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.utimensat

internal object LinuxX64SetTimestamp : PosixOperationHandler<SetTimestamp, SetTimestampError, Unit> {
    override fun invoke(input: SetTimestamp): Either<SetTimestampError, Unit> = memScoped {
        val timespec: CArrayPointer<timespec> = allocArray(2)
        timespec[0].set(input.atimeNanoseconds)
        timespec[1].set(input.mtimeNanoseconds)

        val resultCode = utimensat(
            input.baseDirectory.toDirFd(),
            input.path,
            timespec,
            input.getTimensatFlags(),
        )
        return if (resultCode == 0) {
            Unit.right()
        } else {
            errno.errnoToSetTimestampError(input).left()
        }
    }

    private fun SetTimestamp.getTimensatFlags(): Int = if (followSymlinks) {
        0
    } else {
        AT_SYMLINK_NOFOLLOW
    }

    @Suppress("MagicNumber")
    internal fun timespec.set(timeNanoseconds: Long?) {
        if (timeNanoseconds != null) {
            tv_sec = timeNanoseconds / 1_000_000_000L
            tv_nsec = timeNanoseconds % 1_000_000_000L
        } else {
            tv_sec = 0
            tv_nsec = UTIME_OMIT
        }
    }

    private fun Int.errnoToSetTimestampError(request: SetTimestamp): SetTimestampError = when (this) {
        EACCES -> AccessDenied("Access denied. Request: $request")
        EBADF -> BadFileDescriptor("Bad file descriptor ${request.baseDirectory}")
        EINVAL -> InvalidArgument("Invalid argument in `$request`")
        EIO -> IoError("I/o error on readlink `$request`")
        ELOOP -> TooManySymbolicLinks("Too many symlinks while resolving `$request`")
        ENAMETOOLONG -> NameTooLong("Name too long while resolving `$request`")
        ENOENT -> NoEntry("Component of `${request.path}` does not exist")
        ENOTDIR -> NotDirectory("Error while resolving `${request.path}`: not a directory")
        EPERM -> PermissionDenied("Permission denied")
        EROFS -> ReadOnlyFileSystem("Read-only file system")
        ESRCH -> PermissionDenied("Search permission denied")
        else -> InvalidArgument("Error `$this`")
    }
}
