/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.BaseDirectory.CurrentWorkingDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileAccessibilityCheck
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno

public data class CheckAccess(
    public val path: String,
    public val baseDirectory: BaseDirectory = CurrentWorkingDirectory,
    public val mode: FileAccessibilityCheck,
    public val useEffectiveUserId: Boolean = false,
    public val allowEmptyPath: Boolean = false,
    public val followSymlinks: Boolean = true,
) {
    public companion object : FileSystemOperation<CheckAccess, CheckAccessError, Unit>
}

public sealed class CheckAccessError(
    override val errno: Errno,
    override val message: String,
) : FileSystemOperationError {
    public data class AccessDenied(
        override val message: String,
    ) : CheckAccessError(Errno.ACCES, message)

    public data class BadFileDescriptor(
        override val message: String,
    ) : CheckAccessError(Errno.BADF, message)

    public data class FileBusy(
        override val message: String,
    ) : CheckAccessError(Errno.TXTBSY, message)

    public data class InvalidArgument(
        override val message: String,
    ) : CheckAccessError(Errno.INVAL, message)

    public data class IoError(
        override val message: String,
    ) : CheckAccessError(Errno.IO, message)

    public data class NameTooLong(
        override val message: String,
    ) : CheckAccessError(Errno.NAMETOOLONG, message)

    public data class NoEntry(
        override val message: String,
    ) : CheckAccessError(Errno.NOENT, message)

    public data class NotCapable(
        override val message: String,
    ) : CheckAccessError(Errno.NOTCAPABLE, message)

    public data class NotDirectory(
        override val message: String = "Basepath is not a directory",
    ) : CheckAccessError(Errno.NOTDIR, message)

    public data class ReadOnlyFileSystem(
        override val message: String,
    ) : CheckAccessError(Errno.ROFS, message)

    public data class TooManySymbolicLinks(
        override val message: String,
    ) : CheckAccessError(Errno.LOOP, message)
}
