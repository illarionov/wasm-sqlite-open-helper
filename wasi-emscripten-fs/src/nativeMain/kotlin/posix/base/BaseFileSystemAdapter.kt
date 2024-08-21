/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base

import arrow.core.Either
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.FileSystemOperationError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NotImplemented
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation

internal class BaseFileSystemAdapter(
    private val operations: Map<FileSystemOperation<*, *, *>, PosixOperationHandler<*, *, *>>,
) : FileSystem {
    @Suppress("UNCHECKED_CAST")
    override fun <I : Any, E : FileSystemOperationError, R : Any> execute(
        operation: FileSystemOperation<I, E, R>,
        input: I,
    ): Either<E, R> {
        val handler = operations[operation] as PosixOperationHandler<I, E, R>?
        if (handler == null) {
            return NotImplemented.left() as Either<E, R>
        }

        return handler.invoke(input)
    }

    override fun isOperationSupported(operation: FileSystemOperation<*, *, *>): Boolean {
        return operations.containsKey(operation)
    }

    override fun close() = Unit
}
