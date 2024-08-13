/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.nio

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.CloseError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.CloseFd
import java.io.IOException
import kotlin.concurrent.withLock

internal class NioCloseFd(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<CloseFd, CloseError, Unit> {
    override fun invoke(input: CloseFd): Either<CloseError, Unit> = fsState.fsLock.withLock {
        val fileChannel = fsState.fileDescriptors.remove(input.fd)
            .mapLeft { CloseError.BadFileDescriptor(it.message) }
            .getOrElse { badFileDescriptorError ->
                return badFileDescriptorError.left()
            }
        return Either.catch {
            fileChannel.channel.close()
        }.mapLeft {
            when (it) {
                is IOException -> CloseError.IoError("I/O error: ${it.message}")
                else -> throw IllegalStateException("Unexpected error", it)
            }
        }
    }
}
