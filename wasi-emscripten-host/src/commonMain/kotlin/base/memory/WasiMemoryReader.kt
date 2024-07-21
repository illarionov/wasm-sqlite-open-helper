/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.base.memory

import kotlinx.io.Buffer
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystemByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.IovecArray

public fun interface WasiMemoryReader {
    public fun read(
        fd: Fd,
        strategy: ReadWriteStrategy,
        iovecs: IovecArray,
    ): ULong
}

public class DefaultWasiMemoryReader(
    private val memory: Memory,
    private val fileSystem: FileSystem<*>,
    logger: Logger,
) : WasiMemoryReader {
    private val logger: Logger = logger.withTag("DefaultWasiMemoryReader")

    override fun read(fd: Fd, strategy: ReadWriteStrategy, iovecs: IovecArray): ULong {
        logger.v { "read($fd, ${iovecs.iovecList.map { it.bufLen.value }})" }
        val bbufs = iovecs.createBuffers()

        val readBytes = fileSystem.read(fd, bbufs, strategy)
        var bytesLeft = readBytes.toLong()
        for (vecNo in iovecs.iovecList.indices) {
            val vec = iovecs.iovecList[vecNo]
            val bbuf = bbufs[vecNo]
            if (bytesLeft == 0L) {
                break
            }
            val size = minOf(bbuf.length, bytesLeft.toInt())

            // XXX: too many memory copies
            val buffer = Buffer().also {
                it.write(bbuf.array, bbuf.offset, bbuf.offset + size)
            }

            memory.write(buffer, vec.buf, size)
            bytesLeft -= size
        }
        return readBytes
    }

    private fun IovecArray.createBuffers(): List<FileSystemByteBuffer> = List(iovecList.size) {
        FileSystemByteBuffer(ByteArray(iovecList[it].bufLen.value.toInt()))
    }
}
