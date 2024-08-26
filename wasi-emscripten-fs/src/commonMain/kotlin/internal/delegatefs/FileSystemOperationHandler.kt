/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.delegatefs

import arrow.core.Either
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.FileSystemOperationError

internal fun interface FileSystemOperationHandler<
        in I : Any,
        out E : FileSystemOperationError,
        out R : Any,
        > {
    public operator fun invoke(input: I): Either<E, R>
}
