/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.BaseDirectory.CurrentWorkingDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

public data class SetTimestamp(
    public val path: String,
    public val baseDirectory: BaseDirectory = CurrentWorkingDirectory,
    public val atimeNanoseconds: Long?,
    public val mtimeNanoseconds: Long?,
    public val followSymlinks: Boolean,
) {
   public companion object : FileSystemOperation<SetTimestamp, SetTimestampError, Unit>
}

public data class SetTimestampFd(
    public val fd: Fd,
    public val atimeNanoseconds: Long?,
    public val mtimeNanoseconds: Long?,
    public val followSymlinks: Boolean,
) {
   public companion object : FileSystemOperation<SetTimestampFd, SetTimestampError, Unit>
}

public sealed class SetTimestampError(
    override val errno: Errno,
    override val message: String,
) : FileSystemOperationError {
    public data class AccessDenied(override val message: String) : SetTimestampError(Errno.ACCES, message)
    public data class BadFileDescriptor(override val message: String) : SetTimestampError(Errno.BADF, message)
    public data class InvalidArgument(override val message: String) : SetTimestampError(Errno.INVAL, message)
    public data class IoError(override val message: String) : SetTimestampError(Errno.IO, message)
    public data class NameTooLong(override val message: String) : SetTimestampError(Errno.NAMETOOLONG, message)
    public data class NoEntry(override val message: String) : SetTimestampError(Errno.NOENT, message)
    public data class NotDirectory(override val message: String) : SetTimestampError(Errno.NOTDIR, message)
    public data class PermissionDenied(override val message: String) : SetTimestampError(Errno.PERM, message)
    public data class ReadOnlyFileSystem(override val message: String) : SetTimestampError(Errno.ROFS, message)
    public data class TooManySymbolicLinks(override val message: String) : SetTimestampError(Errno.LOOP, message)
}
