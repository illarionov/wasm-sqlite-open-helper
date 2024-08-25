/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import platform.posix.EACCES
import platform.posix.EBADF
import platform.posix.EINVAL
import platform.posix.EIO
import platform.posix.ELOOP
import platform.posix.ENAMETOOLONG
import platform.posix.ENOENT
import platform.posix.ENOMEM
import platform.posix.ENOTDIR
import platform.posix.PATH_MAX
import platform.posix.errno
import platform.posix.stat
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.AccessDenied
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NameTooLong
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NoEntry
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NotDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.ReadLinkError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.TooManySymbolicLinks
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readlink.ReadLink
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base.PosixOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.ext.toDirFd
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.AT_EMPTY_PATH
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.fstatat
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.readlinkat

internal object LinuxReadLink : PosixOperationHandler<ReadLink, ReadLinkError, String> {
    private const val PATH_STEP = 1024
    private val MAX_PATH_SIZE = maxOf(1024 * 1024, PATH_MAX)

    @Suppress("ReturnCount")
    override fun invoke(input: ReadLink): Either<ReadLinkError, String> {
        var bufSize = getInitialBufSize(input)
            .getOrElse { return it.left() }
        do {
            val buf = ByteArray(bufSize)
            val bytesWritten = buf.usePinned {
                readlinkat(
                    input.baseDirectory.toDirFd(),
                    input.path ?: "",
                    it.addressOf(0),
                    (bufSize - 1).toULong(),
                )
            }
            when {
                bytesWritten < 0 -> return errno.errnoToReadLinkError(input).left()
                bytesWritten < bufSize - 1 -> {
                    buf[bytesWritten.toInt()] = 0
                    return buf.decodeToString().right()
                }

                bufSize == MAX_PATH_SIZE -> return ENAMETOOLONG.errnoToReadLinkError(input).left()
                else -> bufSize = (bufSize + PATH_STEP).coerceAtMost(MAX_PATH_SIZE)
            }
        } while (true)
    }

    private fun getInitialBufSize(
        input: ReadLink,
    ): Either<ReadLinkError, Int> = memScoped {
        val statBuf: stat = alloc()
        val exitCode = fstatat(
            input.baseDirectory.toDirFd(),
            input.path ?: "",
            statBuf.ptr,
            AT_EMPTY_PATH,
        )
        if (exitCode < 0) {
            return errno.errnoToReadLinkError(input).left()
        }

        return statBuf.st_size.let {
            if (it != 0L) {
                it.toInt() + 1
            } else {
                PATH_MAX
            }
        }.right()
    }

    private fun Int.errnoToReadLinkError(request: ReadLink): ReadLinkError = when (this) {
        EACCES -> AccessDenied("Access denied for `$request`")
        EBADF -> BadFileDescriptor("Bad file descriptor ${request.baseDirectory}")
        EINVAL -> InvalidArgument("Invalid argument in `$request`")
        EIO -> IoError("I/o error on readlink `$request`")
        ELOOP -> TooManySymbolicLinks("Too many symlinks while resolving `$request`")
        ENAMETOOLONG -> NameTooLong("Name too long while resolving `$request`")
        ENOENT -> NoEntry("Component of `${request.path}` does not exist")
        ENOMEM -> IoError("No memory")
        ENOTDIR -> NotDirectory("Error while resolving `${request.path}`: not a directory")
        else -> InvalidArgument("Error `$this`")
    }
}
