/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.linux

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import platform.posix.EACCES
import platform.posix.EBADF
import platform.posix.EINVAL
import platform.posix.EIO
import platform.posix.ELOOP
import platform.posix.ENAMETOOLONG
import platform.posix.ENOENT
import platform.posix.ENOMEM
import platform.posix.ENOTDIR
import platform.posix.ENOTSUP
import platform.posix.EPERM
import platform.posix.EROFS
import platform.posix.errno
import platform.posix.fchmod
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.AccessDenied
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.ChmodError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NameTooLong
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NoEntry
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NotDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NotSupported
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.ReadOnlyFileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.TooManySymbolicLinks
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.delegatefs.FileSystemOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.chmod.ChmodFd

internal object LinuxChmodFd : FileSystemOperationHandler<ChmodFd, ChmodError, Unit> {
    override fun invoke(input: ChmodFd): Either<ChmodError, Unit> {
        val resultCode = fchmod(input.fd.fd, input.mode.mask)
        return if (resultCode == 0) {
            Unit.right()
        } else {
            errno.errnoToChmodFdError(input).left()
        }
    }

    private fun Int.errnoToChmodFdError(request: ChmodFd): ChmodError = when (this) {
        EACCES -> AccessDenied("Access denied for `$request`")
        EBADF -> BadFileDescriptor("Bad file descriptor ${request.fd}")
        EINVAL -> InvalidArgument("Invalid argument in `$request`")
        EIO -> IoError("I/O error")
        ELOOP -> TooManySymbolicLinks("Too many symlinks while resolving `$request`")
        ENAMETOOLONG -> NameTooLong("Name too long while resolving `$request`")
        ENOENT -> NoEntry("Component of `$request` does not exist")
        ENOMEM -> IoError("No memory")
        ENOTDIR -> NotDirectory("Error while resolving `$request`: not a directory")
        ENOTSUP -> NotSupported("Flag not supported. Request: `$request`")
        EPERM -> AccessDenied("file is immutable or append-only. Request: `$request`")
        EROFS -> ReadOnlyFileSystem(
            "Write permission requested for a file on a read-only filesystem. Request: `$request`",
        )

        else -> InvalidArgument("Error `$this`")
    }
}
