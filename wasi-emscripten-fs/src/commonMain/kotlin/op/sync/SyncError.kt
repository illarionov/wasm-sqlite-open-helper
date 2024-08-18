/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.sync

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperationError

public sealed class SyncError(
    override val errno: Errno,
    override val message: String,
) : FileSystemOperationError {
    public data class BadFileDescriptor(
        override val message: String,
    ) : SyncError(Errno.BADF, message)

    public data class InvalidArgument(
        override val message: String,
    ) : SyncError(Errno.INVAL, message)

    public data class IoError(
        override val message: String,
    ) : SyncError(Errno.IO, message)
}
