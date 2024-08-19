/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.DefaultWasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.readCatching
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.JvmNioFileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.op.RunWithChannelFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperationError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.IovecArray
import java.nio.channels.Channels
import java.nio.channels.FileChannel

internal class GraalInputStreamWasiMemoryReader(
    private val memory: GraalvmWasmHostMemoryAdapter,
    private val fileSystem: JvmNioFileSystem,
    logger: Logger,
) : WasiMemoryReader {
    private val wasmMemory get() = memory.wasmMemory
    private val defaultMemoryReader = DefaultWasiMemoryReader(memory, fileSystem, logger)

    override fun read(
        fd: Fd,
        strategy: ReadWriteStrategy,
        iovecs: IovecArray,
    ): Either<ReadError, ULong> {
        return if (strategy == CHANGE_POSITION && fileSystem.isOperationSupported(RunWithChannelFd)) {
            val op = RunWithChannelFd(
                fd = fd,
                block = { readChangePosition(it, iovecs) },
            )
            fileSystem.execute(RunWithChannelFd.key(), op)
                .mapLeft { it as ReadError }
        } else {
            defaultMemoryReader.read(fd, strategy, iovecs)
        }
    }

    private fun readChangePosition(
        channelResult: Either<FileSystemOperationError.BadFileDescriptor, FileChannel>,
        iovecs: IovecArray,
    ): Either<ReadError, ULong> {
        val channel = channelResult.mapLeft {
            ReadError.BadFileDescriptor(it.message)
        }.getOrElse {
            return it.left()
        }
        return readCatching {
            var totalBytesRead: ULong = 0U
            val inputStream = Channels.newInputStream(channel).buffered()
            for (vec in iovecs.iovecList) {
                val limit = vec.bufLen.value.toInt()
                val bytesRead = wasmMemory.copyFromStream(memory.node, inputStream, vec.buf.addr, limit)
                if (bytesRead > 0) {
                    totalBytesRead += bytesRead.toULong()
                }
                if (bytesRead < limit) {
                    break
                }
            }
            totalBytesRead
        }
    }
}
