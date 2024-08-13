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
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.WriteError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.WriteFd
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.asByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.writeCatching
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.ChannelPositionError
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.ChannelPositionError.ClosedChannel
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.ChannelPositionError.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.ChannelPositionError.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.FdFileChannel
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.getPosition
import kotlin.concurrent.withLock

internal class NioWriteFd(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<WriteFd, WriteError, ULong> {
    override fun invoke(input: WriteFd): Either<WriteError, ULong> = fsState.fsLock.withLock {
        val channel = fsState.fileDescriptors.get(input.fd)
            ?: return WriteError.BadFileDescriptor(fileDescriptorNotOpenedMessage(input.fd)).left()
        return when (input.strategy) {
            DO_NOT_CHANGE_POSITION -> writeDoNotChangePosition(channel, input.cIovecs)
            CHANGE_POSITION -> writeChangePosition(channel, input.cIovecs)
        }
    }

    private fun writeDoNotChangePosition(
        channel: FdFileChannel,
        cIovecs: List<FileSystemByteBuffer>,
    ): Either<WriteError, ULong> = either {
        var position = channel.getPosition()
            .mapLeft { it.toWriteError() }
            .bind()

        var totalBytesWritten = 0UL
        for (ciovec in cIovecs) {
            val byteBuffer = ciovec.asByteBuffer()
            val bytesWritten = writeCatching {
                channel.channel.write(byteBuffer, position)
            }.bind()
            if (bytesWritten > 0) {
                position += bytesWritten
                totalBytesWritten += bytesWritten.toULong()
            }
            if (bytesWritten < byteBuffer.limit()) {
                break
            }
        }
        totalBytesWritten
    }

    private fun writeChangePosition(
        channel: FdFileChannel,
        cIovecs: List<FileSystemByteBuffer>,
    ): Either<WriteError, ULong> {
        val byteBuffers = Array(cIovecs.size) { cIovecs[it].asByteBuffer() }
        return writeCatching {
            channel.channel.write(byteBuffers).toULong()
        }
    }

    private companion object {
        private fun ChannelPositionError.toWriteError(): WriteError = when (this) {
            is ClosedChannel -> WriteError.IoError(message)
            is InvalidArgument -> WriteError.InvalidArgument(message)
            is IoError -> WriteError.IoError(message)
        }
    }
}
