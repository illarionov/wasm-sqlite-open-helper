/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.base.memory

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystemByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

public fun interface WasiMemoryWriter {
    public fun write(
        fd: Fd,
        strategy: ReadWriteStrategy,
        cioVecs: CiovecArray,
    ): ULong
}

public class DefaultWasiMemoryWriter(
    private val memory: ReadOnlyMemory,
    private val fileSystem: FileSystem<*>,
    logger: Logger,
) : WasiMemoryWriter {
    private val logger: Logger = logger.withTag("DefaultWasiMemoryWriter")
    override fun write(fd: Fd, strategy: ReadWriteStrategy, cioVecs: CiovecArray): ULong {
        logger.v { "write($fd, ${cioVecs.ciovecList.map { it.bufLen.value }})" }
        val bufs = cioVecs.toByteBuffers(memory)
        return fileSystem.write(fd, bufs, strategy)
    }

    private fun CiovecArray.toByteBuffers(
        memory: ReadOnlyMemory,
    ): List<FileSystemByteBuffer> = List(ciovecList.size) { idx ->
        val ciovec = ciovecList[idx]
        val bytesBuffer = Buffer().also {
            memory.read(ciovec.buf, it, ciovec.bufLen.value.toInt())
        }
        // XXX: too many memory copies
        FileSystemByteBuffer(bytesBuffer.readByteArray())
    }
}
