/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op

import ru.pixnews.wasm.sqlite.open.helper.host.include.StructFlock
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

public data class AddAdvisoryLockFd(
    public val fd: Fd,
    public val flock: StructFlock,
) {
    public companion object : FileSystemOperation<AddAdvisoryLockFd, AdvisoryLockError, Unit>
}

public data class RemoveAdvisoryLockFd(
    public val fd: Fd,
    public val flock: StructFlock,
) {
    public companion object : FileSystemOperation<RemoveAdvisoryLockFd, AdvisoryLockError, Unit>
}

public interface AdvisoryLockError : FileSystemOperationError {
    override val errno: Errno
    override val message: String

    public data class Again(
        override val message: String = "Already locked",
        override val errno: Errno = Errno.ACCES,
    ) : AdvisoryLockError

    public data class BadFileDescriptor(
        override val message: String,
        override val errno: Errno = Errno.BADF,
    ) : AdvisoryLockError

    public data class InvalidArgument(
        override val message: String,
        override val errno: Errno = Errno.INVAL,
    ) : AdvisoryLockError

    public data class IoError(
        override val message: String,
        override val errno: Errno = Errno.IO,
    ) : AdvisoryLockError

    public data class NoTty(
        override val message: String,
        override val errno: Errno = Errno.NOTTY,
    ) : AdvisoryLockError

    public data class NoLock(
        override val message: String,
        override val errno: Errno = Errno.NOLCK,
    ) : AdvisoryLockError

    public data class NotSupported(
        override val message: String,
        override val errno: Errno = Errno.NOTSUP,
    ) : AdvisoryLockError

    public data class Overflow(
        override val message: String,
        override val errno: Errno = Errno.OVERFLOW,
    ) : AdvisoryLockError
}
