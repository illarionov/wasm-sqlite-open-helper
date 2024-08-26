/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.FileSystemOperationError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.delegatefs.FileSystemOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.op.RunWithChannelFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.Messages.fileDescriptorNotOpenedMessage
import kotlin.concurrent.withLock

internal class NioRunWithRawChannelFd<R : Any> internal constructor(
    private val fsState: NioFileSystemState,
) : FileSystemOperationHandler<RunWithChannelFd<R>, FileSystemOperationError, R> {
    override fun invoke(input: RunWithChannelFd<R>): Either<FileSystemOperationError, R> = fsState.fsLock.withLock {
        val channel = fsState.fileDescriptors.get(input.fd)
            ?: return input.block(BadFileDescriptor(fileDescriptorNotOpenedMessage(input.fd)).left())
        return channel.lock.withLock {
            input.block(channel.channel.right())
        }
    }
}
