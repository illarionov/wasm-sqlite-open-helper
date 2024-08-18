/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.base.memory

import arrow.core.Either
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.FileSystemByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.WriteError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.WriteFd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CiovecArray

public fun interface WasiMemoryWriter {
    public fun write(
        fd: Fd,
        strategy: ReadWriteStrategy,
        cioVecs: CiovecArray,
    ): Either<WriteError, ULong>
}

public class DefaultWasiMemoryWriter(
    private val memory: ReadOnlyMemory,
    private val fileSystem: FileSystem,
    logger: Logger,
) : WasiMemoryWriter {
    private val logger: Logger = logger.withTag("DefaultWasiMemoryWriter")
    override fun write(fd: Fd, strategy: ReadWriteStrategy, cioVecs: CiovecArray): Either<WriteError, ULong> {
        logger.v { "write($fd, ${cioVecs.ciovecList.map { it.bufLen.value }})" }
        val bufs = cioVecs.toByteBuffers(memory)
        return fileSystem.execute(WriteFd, WriteFd(fd, bufs, strategy))
    }

    private fun CiovecArray.toByteBuffers(
        memory: ReadOnlyMemory,
    ): List<FileSystemByteBuffer> = List(ciovecList.size) { idx ->
        val ciovec = ciovecList[idx]
        // XXX: too many memory copies
        val maxSize = ciovec.bufLen.value.toInt()
        val bytesBuffer = memory.sourceWithMaxSize(ciovec.buf, maxSize).buffered().use {
            it.readByteArray(maxSize)
        }
        FileSystemByteBuffer(bytesBuffer)
    }
}
