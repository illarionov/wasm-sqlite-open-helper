/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.BaseDirectory.CurrentWorkingDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

public data class Chmod(
    val path: String,
    public val baseDirectory: BaseDirectory = CurrentWorkingDirectory,
    public val mode: FileMode,
    public val followSymlinks: Boolean = true,
) {
    public companion object : FileSystemOperation<Chmod, ChmodError, Unit>
}

public data class ChmodFd(
    public val fd: Fd,
    public val mode: FileMode,
) {
    public companion object : FileSystemOperation<ChmodFd, ChmodError, Unit>
}

public sealed class ChmodError(
    override val errno: Errno,
    override val message: String,
) : FileSystemOperationError {
    public data class AccessDenied(
        override val message: String,
    ) : ChmodError(Errno.ACCES, message)

    public data class BadFileDescriptor(
        override val message: String,
    ) : ChmodError(Errno.BADF, message)

    public data class InvalidArgument(
        override val message: String,
    ) : ChmodError(Errno.INVAL, message)

    public data class IoError(
        override val message: String,
    ) : ChmodError(Errno.IO, message)

    public data class NameTooLong(
        override val message: String,
    ) : ChmodError(Errno.NAMETOOLONG, message)

    public data class NoEntry(
        override val message: String,
    ) : ChmodError(Errno.NOENT, message)

    public data class NotCapable(
        override val message: String,
    ) : ChmodError(Errno.NOTCAPABLE, message)

    public data class NotDirectory(
        override val message: String,
    ) : ChmodError(Errno.NOTDIR, message)

    public data class NotSupported(
        override val message: String,
    ) : ChmodError(Errno.NOTSUP, message)

    public data class PermissionDenied(
        override val message: String,
    ) : ChmodError(Errno.PERM, message)

    public data class ReadOnlyFileSystem(
        override val message: String,
    ) : ChmodError(Errno.ROFS, message)

    public data class TooManySymbolicLinks(
        override val message: String,
    ) : ChmodError(Errno.LOOP, message)
}
