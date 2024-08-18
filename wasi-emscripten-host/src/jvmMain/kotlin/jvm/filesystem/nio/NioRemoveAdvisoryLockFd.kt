/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.nio

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.common.ChannelPositionError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.common.ChannelPositionError.ClosedChannel
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.common.ChannelPositionError.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.common.ChannelPositionError.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.AdvisoryLockError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.RemoveAdvisoryLockFd
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructFlock
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.NioFileHandle
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.resolveWhencePosition
import java.io.IOException
import java.nio.channels.ClosedChannelException
import java.nio.channels.FileLock
import kotlin.concurrent.withLock

internal class NioRemoveAdvisoryLockFd(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<RemoveAdvisoryLockFd, AdvisoryLockError, Unit> {
    override fun invoke(input: RemoveAdvisoryLockFd): Either<AdvisoryLockError, Unit> {
        val channel = fsState.fileDescriptors.get(input.fd)
            ?: return AdvisoryLockError.BadFileDescriptor("File descriptor ${input.fd} is not opened").left()

        return removeAdvisoryLock(channel, input.flock)
    }
}

internal fun removeAdvisoryLock(
    channel: NioFileHandle,
    flock: StructFlock,
): Either<AdvisoryLockError, Unit> {
    val position = channel.resolveWhencePosition(flock.l_start.toLong(), flock.whence)
        .getOrElse {
            return it.toAdvisoryLockError().left()
        }

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

    val releaseErrors: List<Either<AdvisoryLockError, Unit>> = locksToRelease.map { fileLock ->
        Either.catch {
            fileLock.release()
        }.mapLeft {
            when (it) {
                is ClosedChannelException -> AdvisoryLockError.BadFileDescriptor("Channel `$fileLock` already closed")
                is IOException -> AdvisoryLockError.IoError("I/O error (${it.message})")
                else -> throw IllegalStateException("Unexpected error", it)
            }
        }
    }

    return releaseErrors.firstOrNull { it.isLeft() } ?: Unit.right()
}

internal fun ChannelPositionError.toAdvisoryLockError(): AdvisoryLockError = when (this) {
    is ClosedChannel -> AdvisoryLockError.BadFileDescriptor(message)
    is InvalidArgument -> AdvisoryLockError.InvalidArgument(message)
    is IoError -> AdvisoryLockError.IoError(message)
}
