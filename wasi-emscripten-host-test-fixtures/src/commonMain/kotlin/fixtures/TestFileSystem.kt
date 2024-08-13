/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.test.fixtures

import arrow.core.Either
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperationError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperationError.NotImplemented

public open class TestFileSystem : FileSystem {
    private val operations: MutableMap<FileSystemOperation<*, *, *>, OperationHandler<*, *, *>> =
        mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    override fun <I : Any, E : FileSystemOperationError, R : Any> execute(
        operation: FileSystemOperation<I, E, R>,
        input: I,
    ): Either<E, R> {
        val handler = operations[operation] as OperationHandler<I, E, R>?
            ?: return NotImplemented.left() as Either<E, R>

        return handler.invoke(input)
    }

    override fun close(): Unit = Unit

    override fun isOperationSupported(operation: FileSystemOperation<*, *, *>): Boolean {
        return operations.containsKey(operation)
    }

    public fun <
            O : FileSystemOperation<I, E, R>,
            I : Any,
            E : FileSystemOperationError,
            R : Any,
            > onOperation(
        operation: O,
        block: OperationHandler<I, E, R>,
    ) {
        operations[operation] = block
    }

    public fun interface OperationHandler<
            in I : Any,
            out E : FileSystemOperationError,
            out R : Any,
            > {
        public operator fun invoke(input: I): Either<E, R>
    }
}
