/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import platform.posix.EACCES
import platform.posix.EBADF
import platform.posix.EBUSY
import platform.posix.EINVAL
import platform.posix.EIO
import platform.posix.EISDIR
import platform.posix.ELOOP
import platform.posix.ENAMETOOLONG
import platform.posix.ENOENT
import platform.posix.ENOMEM
import platform.posix.ENOTDIR
import platform.posix.EPERM
import platform.posix.EROFS
import platform.posix.errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.AccessDenied
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Busy
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NameTooLong
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NoEntry
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NotDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.PathIsDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.PermissionDenied
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.TooManySymbolicLinks
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.UnlinkError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.unlink.UnlinkFile
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base.PosixOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.ext.toDirFd
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.unlinkat

internal object LinuxUnlinkFile : PosixOperationHandler<UnlinkFile, UnlinkError, Unit> {
    override fun invoke(input: UnlinkFile): Either<UnlinkError, Unit> {
        val resultCode = unlinkat(
            input.baseDirectory.toDirFd(),
            input.path,
            0,
        )
        return if (resultCode == 0) {
            Unit.right()
        } else {
            errno.errnoToUnlinkFileError(input).left()
        }
    }

    @Suppress("CyclomaticComplexMethod")
    fun Int.errnoToUnlinkFileError(request: UnlinkFile): UnlinkError = when (this) {
        EACCES -> AccessDenied("Cannot unlink file, access denied. Request: $request")
        EBADF -> BadFileDescriptor("Bad file descriptor `${request.baseDirectory}`")
        EBUSY -> Busy("Cannot delete file because it is being used by another process. Request: $request")
        EINVAL -> InvalidArgument("Invalid flag value specified in unlinkat()")
        EIO -> IoError("Cannot delete file: I/O error.`")
        EISDIR -> PathIsDirectory("`$request` refers to directory")
        ELOOP -> TooManySymbolicLinks("Too many symlinks while resolving `$request`")
        ENAMETOOLONG -> NameTooLong("Name too long while resolving `$request`")
        ENOENT -> NoEntry("Component of `${request.path}` does not exist or empty")
        ENOMEM -> IoError("No memory")
        ENOTDIR -> NotDirectory("`${request.path}` is not a directory")
        EPERM -> PermissionDenied("Can not delete `$request`: permission denied")
        EROFS -> InvalidArgument("Can node delete file: read-only file system")
        else -> InvalidArgument("Other error. Errno: $this")
    }
}
