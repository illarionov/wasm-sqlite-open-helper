/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("WRONG_MULTIPLE_MODIFIERS_ORDER")

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem

import arrow.core.Either
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.FileSystemOperationError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation

public interface FileSystemInterceptor {
    public fun <I : Any, E : FileSystemOperationError, R : Any> intercept(
        chain: Chain<I, E, R>,
    ): Either<E, R>

    public interface Chain<I : Any, out E : FileSystemOperationError, out R : Any> {
        public val operation: FileSystemOperation<I, E, R>
        public val input: I

        public fun proceed(input: I): Either<E, R>
    }
}
