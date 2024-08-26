/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import arrow.core.Either
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.SyncError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.delegatefs.FileSystemOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.Messages.fileDescriptorNotOpenedMessage
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.sync.SyncFd
import java.io.IOException
import java.nio.channels.ClosedChannelException

internal class NioSync(
    private val fsState: NioFileSystemState,
) : FileSystemOperationHandler<SyncFd, SyncError, Unit> {
    override fun invoke(input: SyncFd): Either<SyncError, Unit> {
        val channel = fsState.fileDescriptors.get(input.fd)
            ?: return BadFileDescriptor(fileDescriptorNotOpenedMessage(input.fd)).left()
        return Either.catch {
            channel.channel.force(input.syncMetadata)
        }.mapLeft {
            when (it) {
                is ClosedChannelException -> BadFileDescriptor(fileDescriptorNotOpenedMessage(input.fd))
                is IOException -> IoError("I/O error: ${it.message}")
                else -> throw IllegalStateException("Unexpected error", it)
            }
        }
    }
}
