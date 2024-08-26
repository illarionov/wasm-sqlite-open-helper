/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.linux

import arrow.core.Either
import arrow.core.left
import kotlinx.cinterop.CPointer
import platform.posix.EAGAIN
import platform.posix.EBADF
import platform.posix.EDQUOT
import platform.posix.EFBIG
import platform.posix.EINTR
import platform.posix.EINVAL
import platform.posix.EIO
import platform.posix.ENOSPC
import platform.posix.ENXIO
import platform.posix.EPERM
import platform.posix.EPIPE
import platform.posix.SEEK_CUR
import platform.posix.errno
import platform.posix.iovec
import platform.posix.lseek
import platform.posix.pwritev
import platform.posix.writev
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Again
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.DiskQuota
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.FileTooBig
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Interrupted
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NoSpace
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Nxio
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.PermissionDenied
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Pipe
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.WriteError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.delegatefs.FileSystemOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.linux.LinuxReadFd.callReadWrite
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.FileSystemByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadWriteStrategy.DO_NOT_CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.WriteFd

internal object LinuxWriteFd : FileSystemOperationHandler<WriteFd, WriteError, ULong> {
    override fun invoke(input: WriteFd): Either<WriteError, ULong> {
        return when (input.strategy) {
            CHANGE_POSITION -> callReadWrite(input.fd, input.cIovecs) { fd: Fd, iovecs: CPointer<iovec>, size: Int ->
                writev(fd.fd, iovecs, size)
            }.mapLeft { errNo -> errNo.errnoToWriteError(input.fd, input.cIovecs) }

            DO_NOT_CHANGE_POSITION -> {
                val currentPosition = lseek(input.fd.fd, 0, SEEK_CUR)
                if (currentPosition < 0) {
                    errno.errnoSeekToWriteError(input.fd).left()
                } else {
                    callReadWrite(input.fd, input.cIovecs) { fd: Fd, iovecs: CPointer<iovec>, size: Int ->
                        pwritev(fd.fd, iovecs, size, currentPosition)
                    }.mapLeft { errNo -> errNo.errnoToWriteError(input.fd, input.cIovecs) }
                }
            }
        }
    }

    private fun Int.errnoToWriteError(
        fd: Fd,
        iovecs: List<FileSystemByteBuffer>,
    ): WriteError = when (this) {
        EAGAIN -> Again("Blocking write on non-blocking descriptor")
        EBADF -> BadFileDescriptor("Cannot write to $fd: bad file descriptor")
        EDQUOT -> DiskQuota("Cannot write to $fd: disk quota has been exhausted")
        EFBIG -> FileTooBig("Cannot write to $fd: file exceeds maximum size")
        EINTR -> Interrupted("Write operation interrupted on $fd")
        EINVAL -> InvalidArgument("Invalid argument in request `$fd, $iovecs`")
        EIO -> IoError("I/o error on write to $fd")
        ENOSPC -> NoSpace("Cannot write to $fd: no space")
        EPERM -> PermissionDenied("Cannot write to $fd: operation was prevented by a file seal")
        EPIPE -> Pipe("Cannot write to $fd: remove socked closed")
        else -> InvalidArgument("Write error. Errno: `$this`")
    }

    private fun Int.errnoSeekToWriteError(
        fd: Fd,
    ): WriteError = when (this) {
        EBADF -> BadFileDescriptor("Cannot seek on $fd: bad file descriptor")
        EINVAL -> InvalidArgument("seek() failed. Invalid argument. Fd: $fd")
        ENXIO -> Nxio("Trying to seek beyond end of file. Fd: $fd")
        else -> InvalidArgument("Seel failed: unexpected error. Errno: `$this`")
    }
}
