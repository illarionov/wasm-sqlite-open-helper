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
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.DefaultWasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.writeCatching
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.op.RunWithChannelFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperationError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.WriteError
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CiovecArray
import java.nio.channels.Channels
import java.nio.channels.FileChannel

internal class GraalOutputStreamWasiMemoryWriter(
    private val memory: GraalvmWasmHostMemoryAdapter,
    private val fileSystem: FileSystem,
    logger: Logger,
) : WasiMemoryWriter {
    private val logger = logger.withTag("FS:GrWriter")
    private val wasmMemory = memory.wasmMemory
    private val defaultMemoryWriter = DefaultWasiMemoryWriter(memory, fileSystem, logger)

    override fun write(fd: Fd, strategy: ReadWriteStrategy, cioVecs: CiovecArray): Either<WriteError, ULong> {
        return if (strategy == CHANGE_POSITION && fileSystem.isOperationSupported(RunWithChannelFd)) {
            val op = RunWithChannelFd(
                fd = fd,
                block = { writeChangePosition(it, cioVecs) },
            )
            fileSystem.execute(RunWithChannelFd.key(), op)
                .mapLeft { it as WriteError }
        } else {
            defaultMemoryWriter.write(fd, strategy, cioVecs)
        }
    }

    private fun writeChangePosition(
        channelResult: Either<FileSystemOperationError.BadFileDescriptor, FileChannel>,
        cioVecs: CiovecArray,
    ): Either<WriteError, ULong> {
        logger.v { "writeChangePosition($channelResult, ${cioVecs.ciovecList.map { it.bufLen.value }})" }
        val channel = channelResult.mapLeft {
            WriteError.BadFileDescriptor(it.message)
        }.getOrElse {
            return it.left()
        }

        return writeCatching {
            var totalBytesWritten: ULong = 0U
            val outputStream = Channels.newOutputStream(channel).buffered()
            for (vec in cioVecs.ciovecList) {
                val limit = vec.bufLen.value.toInt()
                wasmMemory.copyToStream(memory.node, outputStream, vec.buf.addr, limit)
                totalBytesWritten += limit.toUInt()
            }
            outputStream.flush()
            totalBytesWritten
        }
    }
}
