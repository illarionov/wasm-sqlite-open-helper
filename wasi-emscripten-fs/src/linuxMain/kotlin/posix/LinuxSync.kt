/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import platform.posix.EBADF
import platform.posix.EDQUOT
import platform.posix.EINTR
import platform.posix.EINVAL
import platform.posix.EIO
import platform.posix.ENOSPC
import platform.posix.EROFS
import platform.posix.errno
import platform.posix.fdatasync
import platform.posix.fsync
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Interrupted
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NoSpace
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.SyncError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.sync.SyncFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base.PosixOperationHandler

internal object LinuxSync : PosixOperationHandler<SyncFd, SyncError, Unit> {
    override fun invoke(input: SyncFd): Either<SyncError, Unit> {
        val resultCode = if (input.syncMetadata) {
            fsync(input.fd.fd)
        } else {
            fdatasync(input.fd.fd)
        }

        return if (resultCode == 0) {
            Unit.right()
        } else {
            errno.errNoToSyncError(input).left()
        }
    }

    private fun Int.errNoToSyncError(request: SyncFd): SyncError = when (this) {
        EBADF -> BadFileDescriptor("Bad file descriptor `${request.fd}`")
        EINTR -> Interrupted("Sync interrupted by signal")
        EIO -> IoError("I/o error on sync")
        ENOSPC -> NoSpace("Can not sync: no enough space")
        EROFS, EINVAL -> InvalidArgument("Sync not supported")
        EDQUOT -> NoSpace("Can not sync: no space")
        else -> IoError("Other error. Errno: $errno")
    }
}
