/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.nio

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystemByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy.DO_NOT_CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.Messages.fileDescriptorNotOpenedMessage
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.ReadError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.ReadFd
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.asByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.readCatching
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.ChannelPositionError
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.ChannelPositionError.ClosedChannel
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.ChannelPositionError.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.ChannelPositionError.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.FdFileChannel
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.getPosition
import kotlin.concurrent.withLock

internal class NioReadFd(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<ReadFd, ReadError, ULong> {
    override fun invoke(input: ReadFd): Either<ReadError, ULong> = fsState.fsLock.withLock {
        val channel = fsState.fileDescriptors.get(input.fd)
            ?: return ReadError.BadFileDescriptor(fileDescriptorNotOpenedMessage(input.fd)).left()
        return when (input.strategy) {
            DO_NOT_CHANGE_POSITION -> readDoNotChangePosition(channel, input.iovecs)
            CHANGE_POSITION -> readChangePosition(channel, input.iovecs)
        }
    }

    private fun readDoNotChangePosition(
        channel: FdFileChannel,
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
        channel: FdFileChannel,
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
            is ClosedChannel -> ReadError.IoError(message)
            is InvalidArgument -> ReadError.InvalidArgument(message)
            is IoError -> ReadError.IoError(message)
        }
    }
}
