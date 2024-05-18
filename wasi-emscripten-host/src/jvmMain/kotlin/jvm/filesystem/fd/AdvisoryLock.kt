/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructFlock
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.AGAIN
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.BADF
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.NOLCK
import java.io.IOException
import java.nio.channels.ClosedChannelException
import java.nio.channels.FileLock
import java.nio.channels.NonReadableChannelException
import java.nio.channels.NonWritableChannelException
import java.nio.channels.OverlappingFileLockException
import kotlin.concurrent.withLock

@Suppress("ThrowsCount")
internal fun addAdvisorylock(
    channel: FdFileChannel,
    flock: StructFlock,
) {
    val isSharedLock = flock.l_type == Fcntl.F_RDLCK
    val position = channel.fileSystem.resolveWhencePosition(channel.fd, flock.l_start.toLong(), flock.whence)
    try {
        // Unlock overlapping locks
        removeAdvisoryLock(channel, flock)

        // Lock new
        val lock: FileLock = channel.channel.tryLock(
            position,
            flock.l_len.toLong(),
            isSharedLock,
        ) ?: throw SysException(AGAIN, "Lock held")

        val fileLockKey = FileLockKey(position, flock.l_len.toLong())
        val oldLock = channel.lock.withLock {
            channel.fileLocks.put(fileLockKey, lock)
        }
        try {
            oldLock?.release()
        } catch (ignore: IOException) {
            // ignore
        }
    } catch (iae: IllegalArgumentException) {
        throw SysException(BADF, "Parameter validation failed: ${iae.message}", iae)
    } catch (cce: ClosedChannelException) {
        throw SysException(BADF, "Channel already closed", cce)
    } catch (ofle: OverlappingFileLockException) {
        throw SysException(BADF, "Overlapping lock: ${ofle.message}", ofle)
    } catch (nrce: NonReadableChannelException) {
        throw SysException(BADF, "Channel not open for reading: ${nrce.message}", nrce)
    } catch (nwce: NonWritableChannelException) {
        throw SysException(BADF, "Channel not open for writing: ${nwce.message}", nwce)
    } catch (ioe: IOException) {
        throw SysException(NOLCK, "IO exception: ${ioe.message}", ioe)
    }
}

internal fun removeAdvisoryLock(
    channel: FdFileChannel,
    flock: StructFlock,
) {
    val position = channel.fileSystem.resolveWhencePosition(channel.fd, flock.l_start.toLong(), flock.whence)
    val locksToRelease: List<FileLock> = channel.lock.withLock {
        val locks: MutableList<FileLock> = mutableListOf()
        val iterator = channel.fileLocks.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            val lock = item.value
            if (lock.overlaps(position, flock.l_len.toLong())) {
                iterator.remove()
                locks.add(lock)
            }
        }
        locks
    }

    try {
        locksToRelease.forEach { it.release() }
    } catch (cce: ClosedChannelException) {
        throw SysException(BADF, "Channel already closed", cce)
    } catch (ioe: IOException) {
        throw SysException(NOLCK, "IO exception: ${ioe.message}", ioe)
    }
}
