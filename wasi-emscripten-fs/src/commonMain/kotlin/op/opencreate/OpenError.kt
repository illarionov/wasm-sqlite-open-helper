/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.open

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.FileSystemOperationError

public sealed class OpenError(
    override val errno: Errno,
    override val message: String,
) : FileSystemOperationError {
    public data class AccessDenied(
        override val message: String,
    ) : OpenError(Errno.ACCES, message)

    public data class BadFileDescriptor(
        override val message: String,
    ) : OpenError(Errno.BADF, message)

    public data class DiskQuota(
        override val message: String,
    ) : OpenError(Errno.DQUOT, message)

    public data class Exists(
        override val message: String,
    ) : OpenError(Errno.EXIST, message)

    public data class FileBusy(
        override val message: String,
    ) : OpenError(Errno.TXTBSY, message)

    public data class Interrupted(
        override val message: String,
    ) : OpenError(Errno.INTR, message)

    public data class InvalidArgument(
        override val message: String,
    ) : OpenError(Errno.INVAL, message)

    public data class IoError(
        override val message: String,
    ) : OpenError(Errno.IO, message)

    public data class MFile(
        override val message: String,
    ) : OpenError(Errno.MFILE, message)

    public data class MLink(
        override val message: String,
    ) : OpenError(Errno.MLINK, message)

    public data class NFile(
        override val message: String,
    ) : OpenError(Errno.NFILE, message)

    public data class NameTooLong(
        override val message: String,
    ) : OpenError(Errno.NAMETOOLONG, message)

    public data class NoEntry(
        override val message: String,
    ) : OpenError(Errno.NOENT, message)

    public data class NoSpace(
        override val message: String,
    ) : OpenError(Errno.NOSPC, message)

    public data class NotCapable(
        override val message: String = "Capabilities insufficient",
    ) : OpenError(Errno.NOTCAPABLE, message)

    public data class NotDirectory(
        override val message: String,
    ) : OpenError(Errno.NOTDIR, message)

    public data class NotSupported(
        override val message: String,
    ) : OpenError(Errno.NOTSUP, message)

    public data class NxIo(
        override val message: String,
    ) : OpenError(Errno.NXIO, message)

    public data class PathIsDirectory(
        override val message: String,
    ) : OpenError(Errno.ISDIR, message)

    public data class PermissionDenied(
        override val message: String,
    ) : OpenError(Errno.PERM, message)

    public data class ReadOnlyFileSystem(
        override val message: String,
    ) : OpenError(Errno.ROFS, message)

    public data class TooManySymbolicLinks(
        override val message: String,
    ) : OpenError(Errno.LOOP, message)
}
