/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.linux

import arrow.core.Either
import platform.posix.EACCES
import platform.posix.EBADF
import platform.posix.ENOMEM
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.AccessDenied
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.StatError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.delegatefs.FileSystemOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.StatFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.StructStat

internal expect fun platformFstatFd(fd: Int): Either<Int, StructStat>

internal object LinuxStatFd : FileSystemOperationHandler<StatFd, StatError, StructStat> {
    override fun invoke(input: StatFd): Either<StatError, StructStat> {
        return platformFstatFd(input.fd.fd).mapLeft {
            it.errnoToStatFdError(input)
        }
    }

    private fun Int.errnoToStatFdError(request: StatFd): StatError = when (this) {
        EACCES -> AccessDenied("Access denied for `$request`")
        EBADF -> BadFileDescriptor("Bad file descriptor ${request.fd}")
        ENOMEM -> IoError("No memory")
        else -> InvalidArgument("Error `$this`")
    }
}
