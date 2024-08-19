/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.truncate

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperationError

public sealed class TruncateError(
    override val errno: Errno,
    override val message: String,
) : FileSystemOperationError {
    public data class AccessDenied(override val message: String) : TruncateError(Errno.ACCES, message)
    public data class BadFileDescriptor(override val message: String) : TruncateError(Errno.BADF, message)
    public data class FileBusy(override val message: String) : TruncateError(Errno.TXTBSY, message)
    public data class FileTooBig(override val message: String) : TruncateError(Errno.FBIG, message)
    public data class InvalidArgument(override val message: String) : TruncateError(Errno.INVAL, message)
    public data class IoError(override val message: String) : TruncateError(Errno.IO, message)
    public data class NameTooLong(override val message: String) : TruncateError(Errno.NAMETOOLONG, message)
    public data class NoEntry(override val message: String) : TruncateError(Errno.NOENT, message)
    public data class NotDirectory(override val message: String) : TruncateError(Errno.NOTDIR, message)
    public data class PathIsDirectory(override val message: String) : TruncateError(Errno.ISDIR, message)
    public data class PermissionDenied(override val message: String) : TruncateError(Errno.PERM, message)
    public data class ReadOnlyFileSystem(override val message: String) : TruncateError(Errno.ROFS, message)
    public data class TooManySymbolicLinks(override val message: String) : TruncateError(Errno.LOOP, message)
}
