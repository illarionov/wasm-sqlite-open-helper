/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.common

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.common.FileDescriptorTable.FileDescriptorError.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.common.FileDescriptorTable.FileDescriptorError.Nfile
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperationError
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.BADF
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.NFILE
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class FileDescriptorTable<V : Any> {
    private val fds: MutableMap<Fd, V> = mutableMapOf()

    fun allocate(
        resourceFactory: (Fd) -> V,
    ): Either<Nfile, V> = getFreeFd()
        .map { fd ->
            val channel = resourceFactory(fd)
            val old = fds.put(fd, channel)
            require(old == null) { "File descriptor $fd already been allocated" }
            channel
        }

    operator fun get(fd: Fd): V? = fds[fd]

    fun release(fd: Fd): Either<BadFileDescriptor, V> {
        return fds.remove(fd)?.right() ?: BadFileDescriptor("Trying to remove already disposed file descriptor").left()
    }

    fun drain(): List<V> {
        val values = fds.values.toList()
        fds.clear()
        return values
    }

    private fun getFreeFd(): Either<Nfile, Fd> {
        for (no in MIN_FD..MAX_FD) {
            if (!fds.containsKey(Fd(no))) {
                return Fd(no).right()
            }
        }
        return Nfile("file descriptor limit exhausted").left()
    }

    internal sealed class FileDescriptorError(
        override val errno: Errno,
        override val message: String,
    ) : FileSystemOperationError {
        internal data class BadFileDescriptor(override val message: String) : FileDescriptorError(BADF, message)
        internal data class Nfile(override val message: String) : FileDescriptorError(NFILE, message)
    }

    companion object {
        const val MIN_FD: Int = 4
        const val MAX_FD: Int = 1024
    }
}
