/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd

import arrow.core.Either
import arrow.core.right
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.common.ChannelPositionError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.common.ChannelPositionError.ClosedChannel
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Whence
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.JvmFileSystemState
import java.io.IOException
import java.nio.channels.ClosedChannelException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.nio.file.Path as NioPath

internal class NioFileHandle(
    val fileSystem: JvmFileSystemState,
    val fd: Fd,
    val path: NioPath,
    val channel: FileChannel,
) {
    val lock: Lock = ReentrantLock()
    val fileLocks: MutableMap<FileLockKey, FileLock> = mutableMapOf()
}

internal fun NioFileHandle.getPosition(): Either<ChannelPositionError, Long> = Either.catch {
    channel.position()
}.mapLeft {
    when (it) {
        is ClosedChannelException -> ClosedChannel("Channel `$path` closed (${it.message})")
        is IOException -> ChannelPositionError.IoError("I/O error: ${it.message}")
        else -> throw IllegalStateException("Unexpected error", it)
    }
}

internal fun NioFileHandle.setPosition(newPosition: Long): Either<ChannelPositionError, Long> = Either.catch {
    channel.position(newPosition)
    newPosition
}.mapLeft {
    when (it) {
        is ClosedChannelException -> ClosedChannel("Channel `$path` closed (${it.message})")
        is IllegalArgumentException -> ChannelPositionError.InvalidArgument("Negative new position (${it.message})")
        is IOException -> ChannelPositionError.IoError("I/O error: ${it.message}")
        else -> throw IllegalStateException("Unexpected error", it)
    }
}

internal fun NioFileHandle.resolveWhencePosition(
    offset: Long,
    whence: Whence,
): Either<ChannelPositionError, Long> = when (whence) {
    Whence.SET -> offset.right()
    Whence.CUR -> this.getPosition().map { it + offset }
    Whence.END -> Either.catch {
        channel.size() - offset
    }.mapLeft {
        when (it) {
            is ClosedChannelException -> ClosedChannel("Channel `$path` closed (${it.message})")
            is IOException -> ChannelPositionError.IoError("I/O error: ${it.message}")
            else -> throw IllegalStateException("Unexpected error", it)
        }
    }
}
