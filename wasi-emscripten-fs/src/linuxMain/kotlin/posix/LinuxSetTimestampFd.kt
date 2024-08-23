/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import platform.posix.EACCES
import platform.posix.EINVAL
import platform.posix.EIO
import platform.posix.EPERM
import platform.posix.EROFS
import platform.posix.errno
import platform.posix.futimens
import platform.posix.timespec
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.AccessDenied
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.PermissionDenied
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.ReadOnlyFileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.SetTimestampError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.settimestamp.SetTimestampFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base.PosixOperationHandler

internal object LinuxSetTimestampFd : PosixOperationHandler<SetTimestampFd, SetTimestampError, Unit> {
    override fun invoke(input: SetTimestampFd): Either<SetTimestampError, Unit> = memScoped {
        val timespec: CPointer<timespec> = allocArray(2)
        timespec[0].set(input.atimeNanoseconds)
        timespec[1].set(input.mtimeNanoseconds)

        val resultCode = futimens(
            input.fd.fd,
            timespec,
        )
        return if (resultCode == 0) {
            Unit.right()
        } else {
            errno.errnoToSetTimestampError(input).left()
        }
    }
}

@Suppress("MagicNumber")
internal fun timespec.set(timeNanoseconds: Long?) {
    if (timeNanoseconds != null) {
        tv_sec = timeNanoseconds / 1_000_000_000L
        tv_nsec = timeNanoseconds % 1_000_000_000L
    } else {
        tv_sec = 0
        tv_nsec = UTIME_OMIT
    }
}

private const val UTIME_OMIT: Long = ((1L shl 30) - 2)

private fun Int.errnoToSetTimestampError(request: SetTimestampFd): SetTimestampError = when (this) {
    EACCES -> AccessDenied("Access denied. Request: $request")
    EINVAL -> InvalidArgument("Invalid argument in `$request`")
    EIO -> IoError("I/o error on readlink `$request`")
    EPERM -> PermissionDenied("Permission denied")
    EROFS -> ReadOnlyFileSystem("Read-only file system")
    else -> InvalidArgument("Error `$this`")
}
