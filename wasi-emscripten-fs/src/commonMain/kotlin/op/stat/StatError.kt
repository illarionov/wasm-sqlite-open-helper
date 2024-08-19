/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.FileSystemOperationError

public sealed class StatError(
    override val errno: Errno,
    override val message: String,
) : FileSystemOperationError {
    public data class AccessDenied(
        override val message: String,
    ) : StatError(Errno.ACCES, message)

    public data class BadFileDescriptor(
        override val message: String,
    ) : StatError(Errno.BADF, message)

    public data class IoError(
        override val message: String,
    ) : StatError(Errno.IO, message)

    public data class NameTooLong(
        override val message: String,
    ) : StatError(Errno.NAMETOOLONG, message)

    public data class NoEntry(
        override val message: String,
    ) : StatError(Errno.NOENT, message)

    public data class NotDirectory(
        override val message: String,
    ) : StatError(Errno.NOTDIR, message)

    public data class NotCapable(
        override val message: String,
    ) : StatError(Errno.IO, message)

    public data class TooManySymbolicLinks(
        override val message: String,
    ) : StatError(Errno.LOOP, message)
}
