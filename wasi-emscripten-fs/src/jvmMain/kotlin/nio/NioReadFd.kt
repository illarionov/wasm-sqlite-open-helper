/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.common.ChannelPositionError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.common.ChannelPositionError.ClosedChannel
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.common.ChannelPositionError.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.common.ChannelPositionError.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.FileSystemOperationError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.asByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.readCatching
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.getPosition
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.Messages.fileDescriptorNotOpenedMessage
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.FileSystemByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.ReadError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadWriteStrategy.DO_NOT_CHANGE_POSITION
import kotlin.concurrent.withLock
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument as FileSystemOperationInvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError as FileSystemOperationIoError

internal class NioReadFd(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<ReadFd, ReadError, ULong> {
    override fun invoke(input: ReadFd): Either<ReadError, ULong> = fsState.fsLock.withLock {
        val channel = fsState.fileDescriptors.get(input.fd)
            ?: return BadFileDescriptor(fileDescriptorNotOpenedMessage(input.fd)).left()
        return when (input.strategy) {
            DO_NOT_CHANGE_POSITION -> readDoNotChangePosition(channel, input.iovecs)
            CHANGE_POSITION -> readChangePosition(channel, input.iovecs)
        }
    }

    private fun readDoNotChangePosition(
        channel: ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.NioFileHandle,
        iovecs: List<FileSystemByteBuffer>,
    ): Either<ReadError, ULong> = either {
        var position = channel.getPosition()
            .mapLeft { it.toReadError() }
            .bind()

        var totalBytesRead: ULong = 0U
        for (iovec in iovecs) {
            val byteBuffer = iovec.asByteBuffer()
            val bytesRead = readCatching {
                channel.channel.read(byteBuffer, position)
            }.bind()
            if (bytesRead > 0) {
                position += bytesRead
                totalBytesRead += bytesRead.toULong()
            }
            if (bytesRead < byteBuffer.limit()) {
                break
            }
        }
        totalBytesRead
    }

    private fun readChangePosition(
        channel: ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.NioFileHandle,
        iovecs: List<FileSystemByteBuffer>,
    ): Either<ReadError, ULong> {
        val byteBuffers = Array(iovecs.size) { iovecs[it].asByteBuffer() }
        val bytesRead: Either<ReadError, Long> = readCatching {
            channel.channel.read(byteBuffers)
        }
        return bytesRead.map {
            if (it != -1L) it.toULong() else 0UL
        }
    }

    private companion object {
        private fun ChannelPositionError.toReadError(): ReadError = when (this) {
            is ClosedChannel -> FileSystemOperationIoError(message)
            is InvalidArgument -> FileSystemOperationInvalidArgument(message)
            is IoError -> FileSystemOperationIoError(message)
        }
    }
}
