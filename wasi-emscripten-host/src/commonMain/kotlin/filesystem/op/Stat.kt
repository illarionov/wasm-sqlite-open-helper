/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.BaseDirectory.CurrentWorkingDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

/**
 * Retrieves information about the file at the given [path].
 * If [path] is relative, it will be resolved using the [baseDirectory] provided.
 *
 * By default, all symbolic links are followed. If [followSymlinks] is set to false and [path] is a symbolic link,
 * the status of the symbolic link itself is returned instead of the target file.
 */
public data class Stat(
    val path: String,
    val baseDirectory: BaseDirectory = CurrentWorkingDirectory,
    val followSymlinks: Boolean = true,
) {
    public companion object : FileSystemOperation<Stat, StatError, StructStat>
}

/**
 * Retrieves information about the currently opened file [fd].
 */
public data class StatFd(
    val fd: Fd,
) {
   public companion object : FileSystemOperation<StatFd, StatError, StructStat>
}

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
