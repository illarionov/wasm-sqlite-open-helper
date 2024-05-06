/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.FileLockKey
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.resolvePosition
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructFlock
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructFlock.Companion
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.memory.readPtr
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.AGAIN
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.BADF
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.INVAL
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.NOLCK
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.SUCCESS
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import java.io.IOException
import java.nio.channels.ClosedChannelException
import java.nio.channels.FileLock
import java.nio.channels.NonReadableChannelException
import java.nio.channels.NonWritableChannelException
import java.nio.channels.OverlappingFileLockException
import kotlin.concurrent.withLock

internal class FcntlHandler(
    private val fileSystem: FileSystem,
    rootLogger: Logger,
) {
    private val logger: Logger = rootLogger.withTag("FcntlHandler")
    private val handlers: Map<UInt, FcntlOperationHandler> = mapOf(
        Fcntl.F_SETLK to FcntlSetLockOperation(),
    )

    public fun invoke(
        memory: Memory,
        fd: Fd,
        operation: UInt,
        thirdArg: Int?,
    ): Int {
        val handler = handlers[operation] ?: throw SysException(INVAL, "Fcntl operation $operation not supported")
        val channel = fileSystem.getStreamByFd(fd)
        return handler.invoke(memory, channel, thirdArg)
    }

    internal fun interface FcntlOperationHandler {
        fun invoke(
            memory: Memory,
            channel: FdChannel,
            varArgs: Int?,
        ): Int
    }

    @Suppress("OBJECT_IS_PREFERRED")
    internal inner class FcntlSetLockOperation : FcntlOperationHandler {
        override fun invoke(
            memory: Memory,
            channel: FdChannel,
            varArgs: Int?,
        ): Int {
            val structStatPtr: WasmPtr<StructFlock> = memory.readPtr(WasmPtr(checkNotNull(varArgs)))
            val flockPacked = memory.readBytes(structStatPtr, StructFlock.STRUCT_FLOCK_SIZE)
            val flock = StructFlock.unpack(flockPacked)

            logger.v { "F_SETLK(${channel.fd}, $flock)" }
            return when (flock.l_type) {
                Fcntl.F_RDLCK, Fcntl.F_WRLCK -> lock(channel, flock)
                Fcntl.F_UNLCK -> unlock(channel, flock)
                else -> throw SysException(INVAL, "Unknown flock.l_type `${flock.l_type}`")
            }
        }

        @Suppress("ThrowsCount")
        private fun lock(
            channel: FdChannel,
            flock: StructFlock,
        ): Int {
            val isSharedLock = flock.l_type == Fcntl.F_RDLCK
            val position = channel.resolvePosition(flock.l_start.toLong(), flock.whence)
            try {
                // Unlock overlapping locks
                unlock(channel, flock)

                // Lock new
                val lock: FileLock = channel.channel.tryLock(
                    position,
                    flock.l_len.toLong(),
                    isSharedLock,
                ) ?: return -AGAIN.code

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

            return SUCCESS.code
        }

        private fun unlock(
            channel: FdChannel,
            flock: StructFlock,
        ): Int {
            val position = channel.resolvePosition(flock.l_start.toLong(), flock.whence)
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

            return SUCCESS.code
        }
    }
}
