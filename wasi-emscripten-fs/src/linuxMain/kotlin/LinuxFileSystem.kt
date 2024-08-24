/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem

import arrow.core.Either
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.FileSystemOperationError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base.BaseFileSystemAdapter
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base.PosixFileSystemState
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base.PosixOperationHandler

internal expect fun createPlatformFileSystemOperations(
    fsState: PosixFileSystemState,
): Map<FileSystemOperation<*, *, *>, PosixOperationHandler<*, *, *>>

public class LinuxFileSystem(
    rootLogger: Logger = Logger,
) : FileSystem {
    private val fsState = PosixFileSystemState(rootLogger)
    private val operations: Map<FileSystemOperation<*, *, *>, PosixOperationHandler<*, *, *>> =
        createPlatformFileSystemOperations(fsState)
    private val fsAdapter = BaseFileSystemAdapter(operations)

    override fun close() {
        fsState.close()
    }

    override fun <I : Any, E : FileSystemOperationError, R : Any> execute(
        operation: FileSystemOperation<I, E, R>,
        input: I,
    ): Either<E, R> = fsAdapter.execute(operation, input)

    override fun isOperationSupported(operation: FileSystemOperation<*, *, *>): Boolean {
        return fsAdapter.isOperationSupported(operation)
    }
}
