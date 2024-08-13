/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.nio

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.AddAdvisoryLockFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.AdvisoryLockError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.AdvisoryLockError.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.AdvisoryLockError.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.AdvisoryLockError.NoLock
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructFlock
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.FdFileChannel
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.FileLockKey
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.resolveWhencePosition
import java.io.IOException
import java.nio.channels.ClosedChannelException
import java.nio.channels.FileLock
import java.nio.channels.NonReadableChannelException
import java.nio.channels.NonWritableChannelException
import java.nio.channels.OverlappingFileLockException
import kotlin.concurrent.withLock

internal class NioAddAdvisoryLockFd(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<AddAdvisoryLockFd, AdvisoryLockError, Unit> {
    override fun invoke(input: AddAdvisoryLockFd): Either<AdvisoryLockError, Unit> = fsState.fsLock.withLock {
        val channel = fsState.fileDescriptors.get(input.fd)
            ?: return BadFileDescriptor("File descriptor ${input.fd} is not opened").left()

        return addAdvisorylock(channel, input.flock)
    }

    companion object {
        internal fun addAdvisorylock(
            channel: FdFileChannel,
            flock: StructFlock,
        ): Either<AdvisoryLockError, Unit> {
            val position = channel.resolveWhencePosition(flock.l_start.toLong(), flock.whence)
                .getOrElse {
                    return it.toAdvisoryLockError().left()
                }

            // Unlock overlapping locks
            removeAdvisoryLock(channel, flock)
                .onLeft {
                    return it.left()
                }

            // Lock new
            val isSharedLock = flock.l_type == Fcntl.F_RDLCK
            val lockResult: Either<AdvisoryLockError, FileLock> = Either.catch {
                channel.channel.tryLock(
                    position,
                    flock.l_len.toLong(),
                    isSharedLock,
                )
            }.mapLeft { error ->
                error.toAdvisoryLockError()
            }.flatMap { fileLock ->
                fileLock?.right() ?: AdvisoryLockError.Again("Lock held").left()
            }

            return lockResult.onRight { lock ->
                val fileLockKey = FileLockKey(position, flock.l_len.toLong())
                val oldLock = channel.lock.withLock {
                    channel.fileLocks.put(fileLockKey, lock)
                }
                try {
                    oldLock?.release()
                } catch (ignore: IOException) {
                    // ignore
                }
            }.map { }
        }

        private fun Throwable.toAdvisoryLockError(): AdvisoryLockError {
            val advisoryLockError = when (this) {
                is IllegalArgumentException -> InvalidArgument("Parameter validation failed: $message")
                is ClosedChannelException -> BadFileDescriptor("Channel already closed ($message)")
                is OverlappingFileLockException -> BadFileDescriptor("Overlapping lock: $message")
                is NonReadableChannelException -> BadFileDescriptor("Channel not open for reading: $message")
                is NonWritableChannelException -> BadFileDescriptor("Channel not open for writing: $message")
                is IOException -> NoLock("IO exception: $message")
                else -> InvalidArgument("Unexpected error: $message")
            }
            return advisoryLockError
        }
    }
}
