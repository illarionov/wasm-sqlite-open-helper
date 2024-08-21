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
import platform.posix.EINTR
import platform.posix.EIO
import platform.posix.ENOSPC
import platform.posix.close
import platform.posix.errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.CloseError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Interrupted
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NoSpace
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.close.CloseFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base.PosixFileSystemState
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base.PosixOperationHandler

internal expect fun Int.platformSpecificErrnoToCloseError(fd: Fd): CloseError

internal class PosixCloseFd(
    private val fsState: PosixFileSystemState,
) : PosixOperationHandler<CloseFd, CloseError, Unit> {
    override fun invoke(input: CloseFd): Either<CloseError, Unit> {
        fsState.remove(input.fd)
        val retval = close(input.fd.fd)
        return if (retval == 0) {
            Unit.right()
        } else {
            errno.errnoToCloseError(input.fd).left()
        }
    }

    private fun Int.errnoToCloseError(fd: Fd): CloseError = when (this) {
        EBADF -> BadFileDescriptor("Bad file descriptor $fd")
        EINTR -> Interrupted("Closing $fd interrupted by signal")
        EIO -> IoError("I/O error while closing $fd")
        ENOSPC -> NoSpace("No space to close $fd")
        else -> platformSpecificErrnoToCloseError(fd)
    }
}
