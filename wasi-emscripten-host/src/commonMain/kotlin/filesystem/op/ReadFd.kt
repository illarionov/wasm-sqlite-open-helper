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

public data class ReadFd(
    public val fd: Fd,
    public val iovecs: List<FileSystemByteBuffer>,
    public val strategy: ReadWriteStrategy = ReadWriteStrategy.CHANGE_POSITION,
) {
    public companion object : FileSystemOperation<ReadFd, ReadError, ULong>
}

public sealed class ReadError(
    override val errno: Errno,
    override val message: String,
) : FileSystemOperationError {
    public data class BadFileDescriptor(
        override val message: String,
    ) : ReadError(Errno.BADF, message)

    public data class Busy(
        override val message: String,
    ) : ReadError(Errno.BUSY, message)

    public data class Interrupted(
        override val message: String,
    ) : ReadError(Errno.INTR, message)

    public data class InvalidArgument(
        override val message: String,
    ) : ReadError(Errno.INVAL, message)

    public data class IoError(
        override val message: String,
    ) : ReadError(Errno.IO, message)

    public data class NotSupported(
        override val message: String,
    ) : ReadError(Errno.NOTSUP, message)

    public data class Overflow(
        override val message: String,
    ) : ReadError(Errno.OVERFLOW, message)

    public data class PathIsDirectory(
        override val message: String,
    ) : ReadError(Errno.ISDIR, message)
}
