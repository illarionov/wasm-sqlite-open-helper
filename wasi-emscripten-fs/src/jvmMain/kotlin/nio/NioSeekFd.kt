/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.common.ChannelPositionError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.common.ChannelPositionError.ClosedChannel
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.common.ChannelPositionError.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.common.ChannelPositionError.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.resolveWhencePosition
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.setPosition
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.seek.SeekError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.seek.SeekFd

internal class NioSeekFd(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<SeekFd, SeekError, Long> {
    override fun invoke(input: SeekFd): Either<SeekError, Long> {
        val channel = fsState.fileDescriptors.get(input.fd)
            ?: return SeekError.BadFileDescriptor("File descriptor `${input.fd}` is not opened").left()

        return channel.resolveWhencePosition(input.fileDelta, input.whence)
            .mapLeft { error -> error.toSeekError() }
            .flatMap { newPosition ->
                if (newPosition >= 0) {
                    channel.setPosition(newPosition)
                        .mapLeft { error -> error.toSeekError() }
                } else {
                    SeekError.InvalidArgument("Incorrect new position: $newPosition").left()
                }
            }
    }

    companion object {
        fun ChannelPositionError.toSeekError(): SeekError = when (this) {
            is ClosedChannel -> SeekError.BadFileDescriptor(message)
            is InvalidArgument -> SeekError.BadFileDescriptor(message)
            is IoError -> SeekError.BadFileDescriptor(message)
        }
    }
}
