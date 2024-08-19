/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.checkaccess

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno.ACCES
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno.BADF
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno.INVAL
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno.IO
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno.LOOP
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno.NAMETOOLONG
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno.NOENT
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno.NOTCAPABLE
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno.NOTDIR
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno.ROFS
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno.TXTBSY
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperationError

public sealed class CheckAccessError(
    override val errno: Errno,
    override val message: String,
) : FileSystemOperationError {
    public data class AccessDenied(
        override val message: String,
    ) : CheckAccessError(ACCES, message)

    public data class BadFileDescriptor(
        override val message: String,
    ) : CheckAccessError(BADF, message)

    public data class FileBusy(
        override val message: String,
    ) : CheckAccessError(TXTBSY, message)

    public data class InvalidArgument(
        override val message: String,
    ) : CheckAccessError(INVAL, message)

    public data class IoError(
        override val message: String,
    ) : CheckAccessError(IO, message)

    public data class NameTooLong(
        override val message: String,
    ) : CheckAccessError(NAMETOOLONG, message)

    public data class NoEntry(
        override val message: String,
    ) : CheckAccessError(NOENT, message)

    public data class NotCapable(
        override val message: String,
    ) : CheckAccessError(NOTCAPABLE, message)

    public data class NotDirectory(
        override val message: String = "Basepath is not a directory",
    ) : CheckAccessError(NOTDIR, message)

    public data class ReadOnlyFileSystem(
        override val message: String,
    ) : CheckAccessError(ROFS, message)

    public data class TooManySymbolicLinks(
        override val message: String,
    ) : CheckAccessError(LOOP, message)
}
