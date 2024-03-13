/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.host.filesystem.fd

import ru.pixnews.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.Errno.BADF
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.Errno.NFILE
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.Fd
import java.nio.channels.FileChannel
import java.nio.file.Path

internal class FileDescriptorMap(
    private val fileSystem: FileSystem,
) {
    private val fds: MutableMap<Fd, FdChannel> = mutableMapOf()

    fun add(
        path: Path,
        channel: FileChannel,
    ): FdChannel {
        val fd = getFreeFd()
        return FdChannel(
            fileSystem = fileSystem,
            fd = fd,
            path = path,
            channel = channel,
        ).also { fdChannel ->
            val old = fds.putIfAbsent(fd, fdChannel)
            require(old == null) { "File descriptor $fd already been allocated" }
        }
    }

    fun remove(fd: Fd): FdChannel {
        return fds.remove(fd) ?: throw SysException(BADF, "Trying to remove already disposed file descriptor")
    }

    fun get(
        fd: Fd,
    ): FdChannel? = fds[fd]

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
        const val MIN_FD = 4
        const val MAX_FD = 1024
    }
}
