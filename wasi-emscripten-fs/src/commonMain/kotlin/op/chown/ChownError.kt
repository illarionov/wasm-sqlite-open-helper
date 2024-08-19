/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.chown

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.FileSystemOperationError

public sealed class ChownError(
    override val errno: Errno,
    override val message: String,
) : FileSystemOperationError {
    public data class AccessDenied(
        override val message: String,
    ) : ChownError(Errno.ACCES, message)

    public data class BadFileDescriptor(
        override val message: String,
    ) : ChownError(Errno.BADF, message)

    public data class InvalidArgument(
        override val message: String,
    ) : ChownError(Errno.INVAL, message)

    public data class IoError(
        override val message: String,
    ) : ChownError(Errno.IO, message)

    public data class NameTooLong(
        override val message: String,
    ) : ChownError(Errno.NAMETOOLONG, message)

    public data class NoEntry(
        override val message: String,
    ) : ChownError(Errno.NOENT, message)

    public data class NotCapable(
        override val message: String,
    ) : ChownError(Errno.NOTCAPABLE, message)

    public data class NotDirectory(
        override val message: String,
    ) : ChownError(Errno.NOTDIR, message)

    public data class PermissionDenied(
        override val message: String,
    ) : ChownError(Errno.PERM, message)

    public data class ReadOnlyFileSystem(
        override val message: String,
    ) : ChownError(Errno.ROFS, message)

    public data class TooManySymbolicLinks(
        override val message: String,
    ) : ChownError(Errno.LOOP, message)

    public data class NotSupported(
        override val message: String,
    ) : ChownError(Errno.NOTSUP, message)
}
