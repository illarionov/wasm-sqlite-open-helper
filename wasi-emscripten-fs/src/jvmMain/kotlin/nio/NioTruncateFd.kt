/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import arrow.core.Either
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.Messages.fileDescriptorNotOpenedMessage
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.truncate.TruncateError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.truncate.TruncateFd
import java.io.IOException
import java.nio.channels.ClosedChannelException
import java.nio.channels.NonReadableChannelException

internal class NioTruncateFd(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<TruncateFd, TruncateError, Unit> {
    override fun invoke(input: TruncateFd): Either<TruncateError, Unit> {
        val channel = fsState.fileDescriptors.get(input.fd)
            ?: return TruncateError.BadFileDescriptor(fileDescriptorNotOpenedMessage(input.fd)).left()
        return Either.catch {
            channel.channel.truncate(input.length.toLong())
            // TODO: extend file size to length?
            Unit
        }.mapLeft { error ->
            error.toTruncateError(input.fd)
        }
    }

    private fun Throwable.toTruncateError(fd: Fd): TruncateError = when (this) {
        is NonReadableChannelException -> TruncateError.InvalidArgument("Read-only channel")
        is ClosedChannelException -> TruncateError.BadFileDescriptor(fileDescriptorNotOpenedMessage(fd))
        is IllegalArgumentException -> TruncateError.InvalidArgument("Negative length")
        is IOException -> TruncateError.IoError("I/O Error: $message")
        else -> throw IllegalStateException("Unexpected error", this)
    }
}
