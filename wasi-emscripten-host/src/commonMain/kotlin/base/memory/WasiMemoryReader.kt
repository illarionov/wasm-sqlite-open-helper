/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.base.memory

import arrow.core.Either
import kotlinx.io.buffered
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.FileSystemByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.ReadError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Iovec
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.IovecArray

public fun interface WasiMemoryReader {
    public fun read(
        fd: Fd,
        strategy: ReadWriteStrategy,
        iovecs: IovecArray,
    ): Either<ReadError, ULong>
}

public class DefaultWasiMemoryReader(
    private val memory: Memory,
    private val fileSystem: FileSystem,
    logger: Logger,
) : WasiMemoryReader {
    private val logger: Logger = logger.withTag("DefaultWasiMemoryReader")

    override fun read(fd: Fd, strategy: ReadWriteStrategy, iovecs: IovecArray): Either<ReadError, ULong> {
        logger.v { "read($fd, ${iovecs.iovecList.map { it.bufLen.value }})" }
        val bbufs: List<FileSystemByteBuffer> = iovecs.createBuffers()

        return fileSystem.execute(ReadFd, ReadFd(fd, bbufs, strategy)).onRight { readBytes ->
            var bytesLeft = readBytes.toLong()
            for (vecNo in iovecs.iovecList.indices) {
                if (bytesLeft == 0L) {
                    break
                }
                val vec: Iovec = iovecs.iovecList[vecNo]
                val bbuf = bbufs[vecNo]
                val size = minOf(bbuf.length, bytesLeft.toInt())

                // XXX: too many memory copies
                memory.sinkWithMaxSize(vec.buf, size).buffered().use {
                    it.write(bbuf.array, bbuf.offset, bbuf.offset + size)
                }
                bytesLeft -= size
            }
        }
    }

    private fun IovecArray.createBuffers(): List<FileSystemByteBuffer> = List(iovecList.size) {
        FileSystemByteBuffer(ByteArray(iovecList[it].bufLen.value.toInt()))
    }
}
