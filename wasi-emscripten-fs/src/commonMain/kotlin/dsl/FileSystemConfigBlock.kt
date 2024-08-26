/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.dsl

import arrow.core.Either
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystemInterceptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystemInterceptor.Chain
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.FileSystemOperationError

@FileSystemDsl
public class FileSystemConfigBlock<E : FileSystemEngineConfig> {
    private val _interceptors: MutableList<FileSystemInterceptor> = mutableListOf()
    internal val interceptors: List<FileSystemInterceptor> get() = _interceptors

    internal var engineConfig: E.() -> Unit = {}
        private set

    public fun addInterceptor(interceptor: FileSystemInterceptor) {
        _interceptors += interceptor
    }

    public inline fun addInterceptor(
        crossinline block: (chain: Chain<Any, FileSystemOperationError, Any>) -> Either<FileSystemOperationError, *>,
    ): Unit = addInterceptor(
        object : FileSystemInterceptor {
            override fun <I : Any, E : FileSystemOperationError, R : Any> intercept(
                chain: Chain<I, E, R>,
            ): Either<E, R> {
                @Suppress("UNCHECKED_CAST")
                return block(chain as Chain<Any, FileSystemOperationError, Any>) as Either<E, R>
            }
        },
    )

    public fun engine(block: E.() -> Unit) {
        val oldConfig = engineConfig
        engineConfig = {
            oldConfig()
            block()
        }
    }
}
