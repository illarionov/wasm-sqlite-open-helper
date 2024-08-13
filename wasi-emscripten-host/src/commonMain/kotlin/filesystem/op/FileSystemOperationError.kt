/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op

import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno

public interface FileSystemOperationError {
    public val message: String
    public val errno: Errno

    public data object NotImplemented : FileSystemOperationError {
        override val message: String = "Operation not implemented"
        override val errno: Errno = Errno.NOTSUP
    }

    public data class BadFileDescriptor(
        override val message: String,
        override val errno: Errno = Errno.BADF,
    ) : FileSystemOperationError
}
