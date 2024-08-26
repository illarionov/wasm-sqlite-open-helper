/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem

import arrow.core.Either
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.dsl.FileSystemCommonConfig
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.dsl.FileSystemConfigBlock
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.dsl.FileSystemEngineConfig
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.FileSystemOperationError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation

public fun <E : FileSystemEngineConfig> FileSystem(
    engine: FileSystemEngine<E>,
    block: FileSystemConfigBlock<E>.() -> Unit = {},
): FileSystem {
    val config = FileSystemConfigBlock<E>().apply(block)
    val commonConfig = object : FileSystemCommonConfig {
        override val interceptors: List<FileSystemInterceptor> = config.interceptors
    }
    return engine.create(
        commonConfig = commonConfig,
        engineConfig = config.engineConfig,
    )
}

public interface FileSystem : AutoCloseable {
    public fun <I : Any, E : FileSystemOperationError, R : Any> execute(
        operation: FileSystemOperation<I, E, R>,
        input: I,
    ): Either<E, R>

    public fun isOperationSupported(operation: FileSystemOperation<*, *, *>): Boolean
}
