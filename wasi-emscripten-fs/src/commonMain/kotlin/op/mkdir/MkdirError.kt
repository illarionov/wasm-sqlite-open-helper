/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.mkdir

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperationError

public sealed class MkdirError(
    override val errno: Errno,
    override val message: String,
) : FileSystemOperationError {
    public data class AccessDenied(override val message: String) : MkdirError(Errno.ACCES, message)
    public data class BadFileDescriptor(override val message: String) : MkdirError(Errno.BADF, message)
    public data class DiskQuota(override val message: String) : MkdirError(Errno.DQUOT, message)
    public data class Exist(override val message: String) : MkdirError(Errno.EXIST, message)
    public data class InvalidArgument(override val message: String) : MkdirError(Errno.INVAL, message)
    public data class IoError(override val message: String) : MkdirError(Errno.IO, message)
    public data class Mlink(override val message: String) : MkdirError(Errno.MLINK, message)
    public data class NameTooLong(override val message: String) : MkdirError(Errno.NAMETOOLONG, message)
    public data class NoEntry(override val message: String) : MkdirError(Errno.NOENT, message)
    public data class NoSpace(override val message: String) : MkdirError(Errno.NOSPC, message)
    public data class NotDirectory(override val message: String) : MkdirError(Errno.NOTDIR, message)
    public data class PermissionDenied(override val message: String) : MkdirError(Errno.PERM, message)
    public data class ReadOnlyFileSystem(override val message: String) : MkdirError(Errno.ROFS, message)
    public data class TooManySymbolicLinks(override val message: String) : MkdirError(Errno.LOOP, message)
}
