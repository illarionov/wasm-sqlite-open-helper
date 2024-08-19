/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd

import arrow.core.Either
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Nfile
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.FileDescriptorTable
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.JvmFileSystemState
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class NioFileDescriptorTable(
    private val fsState: JvmFileSystemState,
    logger: Logger,
) : AutoCloseable {
    private val logger = logger.withTag("NioFileDescriptorTable")
    private val fds: FileDescriptorTable<NioFileHandle> = FileDescriptorTable()
    private val lock: Lock = ReentrantLock()

    fun add(
        path: Path,
        channel: FileChannel,
    ): Either<Nfile, NioFileHandle> = lock.withLock {
        fds.allocate { fd ->
            NioFileHandle(
                fileSystem = fsState,
                fd = fd,
                path = path,
                channel = channel,
            )
        }
    }

    fun remove(fd: Fd): Either<BadFileDescriptor, NioFileHandle> = lock.withLock {
        return fds.release(fd)
    }

    fun get(
        fd: Fd,
    ): NioFileHandle? = lock.withLock {
        fds[fd]
    }

    override fun close() {
        val channels = lock.withLock {
             fds.drain()
        }
        for (chan in channels) {
            try {
                chan.channel.close()
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") ex: Exception) {
                logger.i { "close(${chan.path}) failed. Ignore." }
            }
        }
    }
}
