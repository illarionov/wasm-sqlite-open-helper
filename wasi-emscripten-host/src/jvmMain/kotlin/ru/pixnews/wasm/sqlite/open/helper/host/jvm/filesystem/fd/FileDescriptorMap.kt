/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.JvmFileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.JvmPath
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.BADF
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.NFILE
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import java.nio.channels.FileChannel

internal class FileDescriptorMap(
    private val fileSystem: JvmFileSystem,
) {
    private val fds: MutableMap<Fd, FdFileChannel> = mutableMapOf()

    fun add(
        path: JvmPath,
        channel: FileChannel,
    ): FdFileChannel {
        val fd = getFreeFd()
        return FdFileChannel(
            fileSystem = fileSystem,
            fd = fd,
            path = path,
            channel = channel,
        ).also { fdChannel ->
            val old = fds.putIfAbsent(fd, fdChannel)
            require(old == null) { "File descriptor $fd already been allocated" }
        }
    }

    fun remove(fd: Fd): FdFileChannel {
        return fds.remove(fd) ?: throw SysException(BADF, "Trying to remove already disposed file descriptor")
    }

    fun get(
        fd: Fd,
    ): FdFileChannel? = fds[fd]

    @Throws(SysException::class)
    private fun getFreeFd(): Fd {
        for (no in MIN_FD..MAX_FD) {
            if (!fds.containsKey(Fd(no))) {
                return Fd(no)
            }
        }
        throw SysException(NFILE)
    }

    companion object {
        const val MIN_FD: Int = 4
        const val MAX_FD: Int = 1024
    }
}
