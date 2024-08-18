/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.memory

import arrow.core.Either
import com.dylibso.chicory.runtime.Memory
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.isJvmOrAndroidMinApi34
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.trySetAccessibleCompat
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.DefaultWasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.FileSystemByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.IovecArray
import java.lang.reflect.Field
import java.nio.ByteBuffer

internal class ChicoryWasiMemoryReader(
    private val memory: Memory,
    private val fileSystem: FileSystem,
    private val bufferField: Field,
) : WasiMemoryReader {
    override fun read(fd: Fd, strategy: ReadWriteStrategy, iovecs: IovecArray): Either<ReadError, ULong> {
        val memoryByteBuffer = bufferField.get(memory) as? ByteBuffer
            ?: error("Can not get memory byte buffer")
        check(memoryByteBuffer.hasArray()) { "MemoryBuffer without array" }
        val bbufs = iovecs.toByteBuffers(memoryByteBuffer)
        return fileSystem.execute(ReadFd, ReadFd(fd, bbufs, strategy))
    }

    private fun IovecArray.toByteBuffers(
        memoryBuffer: ByteBuffer,
    ): List<FileSystemByteBuffer> = List(iovecList.size) { iovecNo ->
        val ioVec = iovecList[iovecNo]
        FileSystemByteBuffer(
            memoryBuffer.array(),
            memoryBuffer.arrayOffset() + ioVec.buf.addr,
            ioVec.bufLen.value.toInt(),
        )
    }

    companion object {
        fun createOrDefault(
            memory: ChicoryMemoryAdapter,
            fileSystem: FileSystem,
            logger: Logger,
        ): WasiMemoryReader = if (isJvmOrAndroidMinApi34()) {
            tryCreate(memory.wasmMemory, fileSystem)
        } else {
            null
        } ?: DefaultWasiMemoryReader(memory, fileSystem, logger)

        @Suppress("ReturnCount", "SwallowedException")
        fun tryCreate(
            memory: Memory,
            fileSystem: FileSystem,
        ): ChicoryWasiMemoryReader? {
            try {
                val bufferField: Field = Memory::class.java.getDeclaredField("buffer")
                if (!bufferField.trySetAccessibleCompat()) {
                    return null
                }
                if (bufferField.get(memory) !is ByteBuffer) {
                    return null
                }
                return ChicoryWasiMemoryReader(memory, fileSystem, bufferField)
            } catch (nsfe: NoSuchFieldException) {
                return null
            } catch (se: SecurityException) {
                return null
            }
        }
    }
}
