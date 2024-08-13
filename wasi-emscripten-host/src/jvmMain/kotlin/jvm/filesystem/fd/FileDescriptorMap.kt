/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperationError
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.FileDescriptorMap.FileDescriptorError.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.FileDescriptorMap.FileDescriptorError.Nfile
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.nio.JvmFileSystemState
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.BADF
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.NFILE
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class FileDescriptorMap(
    private val fileSystem: JvmFileSystemState,
    logger: Logger,
) : AutoCloseable {
    private val logger = logger.withTag("FDSMap")
    private val fds: MutableMap<Fd, FdFileChannel> = mutableMapOf()
    private val lock: Lock = ReentrantLock()

    fun add(
        path: Path,
        channel: FileChannel,
    ): Either<Nfile, FdFileChannel> = lock.withLock {
        return getFreeFd()
            .onRight { fd ->
                logger.v { "add() fd: $fd" }
            }.map { fd ->
                FdFileChannel(
                    fileSystem = fileSystem,
                    fd = fd,
                    path = path,
                    channel = channel,
                ).also { fdChannel ->
                    val old = fds.putIfAbsent(fd, fdChannel)
                    require(old == null) { "File descriptor $fd already been allocated" }
                }
            }
    }

    fun remove(fd: Fd): Either<BadFileDescriptor, FdFileChannel> = lock.withLock {
        logger.v { "Remove($fd)" }
        return fds.remove(fd)?.right() ?: BadFileDescriptor("Trying to remove already disposed file descriptor").left()
    }

    fun get(
        fd: Fd,
    ): FdFileChannel? = lock.withLock {
        logger.v { "get($fd)" }
        fds[fd]
    }

    override fun close() {
        val channels = lock.withLock {
            val channels = fds.values.toList()
            fds.clear()
            channels
        }
        for (chan in channels) {
            try {
                chan.channel.close()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") ex: Exception) {
                logger.i { "close(${chan.path}) failed. Ignore." }
            }
        }
    }

    private fun getFreeFd(): Either<Nfile, Fd> = lock.withLock {
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
