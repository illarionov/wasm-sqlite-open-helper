/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext

import arrow.core.Either
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.ReadError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.WriteError
import java.io.IOException
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.ClosedChannelException
import java.nio.channels.NonReadableChannelException
import java.nio.channels.NonWritableChannelException

@InternalWasmSqliteHelperApi
public inline fun <R : Any> readCatching(
    block: () -> R,
): Either<ReadError, R> = Either.catch {
    block()
}.mapLeft {
    when (it) {
        is ClosedByInterruptException -> ReadError.IoError("Interrupted")
        is AsynchronousCloseException -> ReadError.IoError("Channel closed on other thread")
        is ClosedChannelException -> ReadError.Interrupted("Channel closed")
        is NonReadableChannelException -> ReadError.BadFileDescriptor("Non readable channel")
        is IOException -> ReadError.IoError("I/O error: ${it.message}")
        else -> throw IllegalStateException("Unexpected error", it)
    }
}

@InternalWasmSqliteHelperApi
public inline fun <R : Any> writeCatching(
    block: () -> R,
): Either<WriteError, R> = Either.catch {
    block()
}.mapLeft {
    when (it) {
        is ClosedByInterruptException -> WriteError.IoError("Interrupted")
        is AsynchronousCloseException -> WriteError.IoError("Channel closed on other thread")
        is ClosedChannelException -> WriteError.Interrupted("Channel closed")
        is NonWritableChannelException -> WriteError.BadFileDescriptor("Non writeable channel")
        is IOException -> WriteError.IoError("I/O error: ${it.message}")
        else -> throw IllegalStateException("Unexpected error", it)
    }
}
