/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.linux

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.Pinned
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.pin
import platform.posix.EAGAIN
import platform.posix.EBADF
import platform.posix.EINTR
import platform.posix.EINVAL
import platform.posix.EIO
import platform.posix.EISDIR
import platform.posix.ENXIO
import platform.posix.SEEK_CUR
import platform.posix.errno
import platform.posix.iovec
import platform.posix.lseek
import platform.posix.preadv
import platform.posix.readv
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Interrupted
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NotSupported
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Nxio
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.PathIsDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.ReadError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.delegatefs.FileSystemOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.FileSystemByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadWriteStrategy.DO_NOT_CHANGE_POSITION

internal object LinuxReadFd : FileSystemOperationHandler<ReadFd, ReadError, ULong> {
    override fun invoke(input: ReadFd): Either<ReadError, ULong> {
        return when (input.strategy) {
            CHANGE_POSITION -> callReadWrite(input.fd, input.iovecs) { fd, iovecs, size ->
                readv(fd.fd, iovecs, size)
            }.mapLeft { errNo -> errNo.errnoToReadError(input.fd, input.iovecs) }

            DO_NOT_CHANGE_POSITION -> {
                val currentPosition = lseek(input.fd.fd, 0, SEEK_CUR)
                if (currentPosition < 0) {
                    errno.errnoSeekToReadError(input.fd).left()
                } else {
                    callReadWrite(input.fd, input.iovecs) { fd, iovecs, size ->
                        preadv(fd.fd, iovecs, size, currentPosition)
                    }.mapLeft { errNo -> errNo.errnoToReadError(input.fd, input.iovecs) }
                }
            }
        }
    }

    internal fun callReadWrite(
        fd: Fd,
        iovecs: List<FileSystemByteBuffer>,
        block: (fd: Fd, iovecs: CArrayPointer<iovec>, size: Int) -> Long,
    ): Either<Int, ULong> {
        if (iovecs.isEmpty()) {
            return 0UL.right()
        }

        val bytesMoved = memScoped {
            val size = iovecs.size
            val posixIovecs: CArrayPointer<iovec> = allocArray(size)
            iovecs.withPinnedByteArrays { pinnedByteArrays ->
                // TODO: check length
                iovecs.forEachIndexed { index, vec ->
                    posixIovecs[index].apply {
                        iov_base = pinnedByteArrays[index].addressOf(vec.offset)
                        iov_len = vec.length.toULong()
                    }
                }
                block(fd, posixIovecs, size)
            }
        }

        return if (bytesMoved >= 0) {
            bytesMoved.toULong().right()
        } else {
            errno.left()
        }
    }

    private inline fun <R : Any> List<FileSystemByteBuffer>.withPinnedByteArrays(
        block: (byteArrays: List<Pinned<ByteArray>>) -> R,
    ): R {
        val pinnedByteArrays: List<Pinned<ByteArray>> = this.map {
            it.array.pin()
        }
        return try {
            block(pinnedByteArrays)
        } finally {
            pinnedByteArrays.forEach(Pinned<ByteArray>::unpin)
        }
    }

    private fun Int.errnoToReadError(
        fd: Fd,
        iovecs: List<FileSystemByteBuffer>,
    ): ReadError = when (this) {
        EAGAIN -> NotSupported("Non-blocking read would block. Request: `$fd, $iovecs`")
        EBADF -> BadFileDescriptor("Can not read on $fd")
        EINTR -> Interrupted("Read operation interrupted on $fd")
        EINVAL -> InvalidArgument("Invalid argument in request `$fd, $iovecs`")
        EIO -> IoError("I/o error on read from $fd")
        EISDIR -> PathIsDirectory("$fd refers to directory")
        else -> InvalidArgument("Read error. Errno: `$this`")
    }

    private fun Int.errnoSeekToReadError(
        fd: Fd,
    ): ReadError = when (this) {
        EBADF -> BadFileDescriptor("Can not seek on $fd")
        EINVAL -> InvalidArgument("seek() failed. Invalid argument. Fd: $fd")
        ENXIO -> Nxio("trying to seek beyond end of file. Fd: $fd")
        else -> InvalidArgument("Seel failed: unexpected error. Errno: `$this`")
    }
}
