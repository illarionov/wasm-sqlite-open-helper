/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.io.lock.ReentrantLock
import ru.pixnews.wasm.sqlite.open.helper.io.lock.reentrantLock
import ru.pixnews.wasm.sqlite.open.helper.io.lock.withLock

internal class PosixFileSystemState : AutoCloseable {
    private val lock: ReentrantLock = reentrantLock()
    private val openFileDescriptors: MutableSet<Fd> = mutableSetOf()

    fun add(
        fd: Fd,
    ): Unit = lock.withLock {
        val added = openFileDescriptors.add(fd)
        require(added) { "File descriptor $fd already been allocated" }
    }

    fun remove(
        fd: Fd,
    ): Unit = lock.withLock {
        openFileDescriptors.remove(fd)
    }

    override fun close() {
        val fileDescriptors = lock.withLock {
            openFileDescriptors.toList()
        }
        for (fd in fileDescriptors) {
            val errNo = platform.posix.close(fd.fd)
            if (errNo != 0) {
                // close($fd) failed with errno $errNo. Ignore.
            }
        }
    }
}
