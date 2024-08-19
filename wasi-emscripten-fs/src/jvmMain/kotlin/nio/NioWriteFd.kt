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
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.WriteError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.asByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.writeCatching
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.getPosition
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.Messages.fileDescriptorNotOpenedMessage
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.FileSystemByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadWriteStrategy.DO_NOT_CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.WriteFd
import kotlin.concurrent.withLock
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument as BaseInvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError as BaseIoError

internal class NioWriteFd(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<WriteFd, WriteError, ULong> {
    override fun invoke(input: WriteFd): Either<WriteError, ULong> = fsState.fsLock.withLock {
        val channel = fsState.fileDescriptors.get(input.fd)
            ?: return BadFileDescriptor(fileDescriptorNotOpenedMessage(input.fd)).left()
        return when (input.strategy) {
            DO_NOT_CHANGE_POSITION -> writeDoNotChangePosition(channel, input.cIovecs)
            CHANGE_POSITION -> writeChangePosition(channel, input.cIovecs)
        }
    }

    private fun writeDoNotChangePosition(
        channel: ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.NioFileHandle,
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
        channel: ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.NioFileHandle,
        cIovecs: List<FileSystemByteBuffer>,
    ): Either<WriteError, ULong> {
        val byteBuffers = Array(cIovecs.size) { cIovecs[it].asByteBuffer() }
        return writeCatching {
            channel.channel.write(byteBuffers).toULong()
        }
    }

    private companion object {
        private fun ChannelPositionError.toWriteError(): WriteError = when (this) {
            is ClosedChannel -> BaseIoError(message)
            is InvalidArgument -> BaseInvalidArgument(message)
            is IoError -> BaseIoError(message)
        }
    }
}
