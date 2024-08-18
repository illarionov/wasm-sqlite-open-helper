/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.cwd

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperationError

public sealed class GetCurrentWorkingDirectoryError(
    override val errno: Errno,
    override val message: String,
) : FileSystemOperationError {
    public data class NoEntry(
        override val message: String = "A component of the pathname no longer exists",
    ) : GetCurrentWorkingDirectoryError(Errno.NOENT, message)

    public data class NameTooLong(
        override val message: String,
    ) : GetCurrentWorkingDirectoryError(Errno.NAMETOOLONG, message)

    public data class AccessDenied(
        override val message: String,
    ) : GetCurrentWorkingDirectoryError(Errno.ACCES, message)
}
