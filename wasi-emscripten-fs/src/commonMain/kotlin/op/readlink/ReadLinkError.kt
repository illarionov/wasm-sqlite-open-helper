/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readlink

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.FileSystemOperationError

public sealed class ReadLinkError(
    override val errno: Errno,
    override val message: String,
) : FileSystemOperationError {
    public data class AccessDenied(
        override val message: String,
    ) : ReadLinkError(Errno.ACCES, message)

    public data class BadFileDescriptor(
        override val message: String,
    ) : ReadLinkError(Errno.BADF, message)

    public data class InvalidArgument(
        override val message: String,
    ) : ReadLinkError(Errno.INVAL, message)

    public data class IoError(
        override val message: String,
    ) : ReadLinkError(Errno.IO, message)

    public data class NameTooLong(
        override val message: String,
    ) : ReadLinkError(Errno.NAMETOOLONG, message)

    public data class NoEntry(
        override val message: String,
    ) : ReadLinkError(Errno.NOENT, message)

    public data class NotDirectory(
        override val message: String,
    ) : ReadLinkError(Errno.NOTDIR, message)

    public data class TooManySymbolicLinks(
        override val message: String,
    ) : ReadLinkError(Errno.LOOP, message)
}
