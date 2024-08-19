/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.SeekError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.resolveWhencePosition
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.setPosition
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.ChannelPositionError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.seek.SeekFd

internal class NioSeekFd(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<SeekFd, SeekError, Long> {
    override fun invoke(input: SeekFd): Either<SeekError, Long> {
        val channel = fsState.fileDescriptors.get(input.fd)
            ?: return BadFileDescriptor("File descriptor `${input.fd}` is not opened").left()

        return channel.resolveWhencePosition(input.fileDelta, input.whence)
            .mapLeft { error -> error.toSeekError() }
            .flatMap { newPosition ->
                if (newPosition >= 0) {
                    channel.setPosition(newPosition)
                        .mapLeft { error -> error.toSeekError() }
                } else {
                    InvalidArgument("Incorrect new position: $newPosition").left()
                }
            }
    }

    companion object {
        fun ChannelPositionError.toSeekError(): SeekError = when (this) {
            is ChannelPositionError.ClosedChannel -> BadFileDescriptor(message)
            is ChannelPositionError.InvalidArgument -> BadFileDescriptor(message)
            is ChannelPositionError.IoError -> BadFileDescriptor(message)
        }
    }
}
