/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.op

import arrow.core.Either
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.FileSystemOperationError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation
import java.nio.channels.FileChannel

public class RunWithChannelFd<R>(
    public val fd: Fd,
    public val block: (
        channel: Either<BadFileDescriptor, FileChannel>,
    ) -> Either<FileSystemOperationError, R>,
) {
    public companion object : FileSystemOperation<RunWithChannelFd<Any>, FileSystemOperationError, Any> {
        override val tag: String = "runwithchanfd"
        public fun <R : Any> key(): FileSystemOperation<RunWithChannelFd<R>, FileSystemOperationError, R> {
            @Suppress("UNCHECKED_CAST")
            return Companion as FileSystemOperation<RunWithChannelFd<R>, FileSystemOperationError, R>
        }
    }
}
