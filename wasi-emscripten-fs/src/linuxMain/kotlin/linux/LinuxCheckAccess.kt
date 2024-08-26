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
import platform.posix.EPERM
import platform.posix.EROFS
import platform.posix.ETXTBSY
import platform.posix.F_OK
import platform.posix.R_OK
import platform.posix.W_OK
import platform.posix.X_OK
import platform.posix.errno
import platform.posix.syscall
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.AccessDenied
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.CheckAccessError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NameTooLong
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NoEntry
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NotDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.ReadOnlyFileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.TextFileBusy
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.TooManySymbolicLinks
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.delegatefs.FileSystemOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.linux.ext.toDirFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.checkaccess.CheckAccess
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.checkaccess.FileAccessibilityCheck
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.checkaccess.FileAccessibilityCheck.EXECUTABLE
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.checkaccess.FileAccessibilityCheck.READABLE
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.checkaccess.FileAccessibilityCheck.WRITEABLE
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.AT_EACCESS
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.AT_EMPTY_PATH
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.AT_SYMLINK_NOFOLLOW
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.SYS_faccessat2

internal object LinuxCheckAccess : FileSystemOperationHandler<CheckAccess, CheckAccessError, Unit> {
    override fun invoke(input: CheckAccess): Either<CheckAccessError, Unit> {
        val resultCode = syscall(
            SYS_faccessat2.toLong(),
            input.baseDirectory.toDirFd(),
            input.path,
            input.mode.toModeFlags(),
            input.toCheckAccessFlags(),
        )
        return if (resultCode == 0L) {
            Unit.right()
        } else {
            errno.errnoToCheckAccessError(input).left()
        }
    }

    private fun Set<FileAccessibilityCheck>.toModeFlags(): Int {
        if (this.isEmpty()) {
            return F_OK
        }
        var mask = 0
        if (this.contains(READABLE)) {
            mask = mask and R_OK
        }
        if (this.contains(WRITEABLE)) {
            mask = mask and W_OK
        }
        if (this.contains(EXECUTABLE)) {
            mask = mask and X_OK
        }
        return mask
    }

    private fun CheckAccess.toCheckAccessFlags(): Int {
        var mask = 0
        if (this.useEffectiveUserId) {
            mask = mask and AT_EACCESS
        }
        if (this.allowEmptyPath) {
            mask = mask and AT_EMPTY_PATH
        }
        if (!this.followSymlinks) {
            mask = mask and AT_SYMLINK_NOFOLLOW
        }
        return mask
    }

    private fun Int.errnoToCheckAccessError(request: CheckAccess): CheckAccessError = when (this) {
        EACCES -> AccessDenied("Access denied for `$request`")
        EBADF -> BadFileDescriptor("Bad file descriptor ${request.baseDirectory}")
        EINVAL -> InvalidArgument("Invalid argument in `$request`")
        EIO -> IoError("I/O error")
        ELOOP -> TooManySymbolicLinks("Too many symlinks while resolving `$request`")
        ENAMETOOLONG -> NameTooLong("Name too long while resolving `$request`")
        ENOENT -> NoEntry("Component of `${request.path}` does not exist")
        ENOMEM -> IoError("No memory")
        ENOTDIR -> NotDirectory("Error while resolving `${request.path}`: not a directory")
        EPERM -> AccessDenied("Write permission requested to a file with immutable flag. Request: `$request`")
        EROFS -> ReadOnlyFileSystem(
            "Write permission requested for a file on a read-only filesystem. Request: `$request`",
        )

        ETXTBSY -> TextFileBusy("Write permission requested to executed file. Request: $request")
        else -> InvalidArgument("Unexpected error `$this`")
    }
}
