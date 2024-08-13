/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystemByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

public data class WriteFd(
    public val fd: Fd,
    public val cIovecs: List<FileSystemByteBuffer>,
    public val strategy: ReadWriteStrategy = ReadWriteStrategy.CHANGE_POSITION,
) {
    public companion object : FileSystemOperation<WriteFd, WriteError, ULong>
}

public sealed class WriteError(
    override val errno: Errno,
    override val message: String,
) : FileSystemOperationError {
    public data class Again(
        override val message: String,
    ) : WriteError(Errno.AGAIN, message)

    public data class BadFileDescriptor(
        override val message: String,
    ) : WriteError(Errno.BADF, message)

    public data class DiskQuota(
        override val message: String,
    ) : WriteError(Errno.DQUOT, message)

    public data class FileTooBig(
        override val message: String,
    ) : WriteError(Errno.FBIG, message)

    public data class Interrupted(
        override val message: String,
    ) : WriteError(Errno.INTR, message)

    public data class InvalidArgument(
        override val message: String,
    ) : WriteError(Errno.INVAL, message)

    public data class IoError(
        override val message: String,
    ) : WriteError(Errno.IO, message)

    public data class NoBufferSpace(
        override val message: String,
    ) : WriteError(Errno.NOBUFS, message)

    public data class NoSpace(
        override val message: String,
    ) : WriteError(Errno.NOSPC, message)

    public data class Pipe(
        override val message: String,
    ) : WriteError(Errno.PIPE, message)

    public data class ReadOnlyFileSystem(
        override val message: String,
    ) : WriteError(Errno.ROFS, message)
}
