/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.memory

import com.dylibso.chicory.runtime.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystemByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.IovecArray
import java.lang.reflect.Field
import java.nio.ByteBuffer

internal class ChicoryWasiMemoryReader(
    private val memory: Memory,
    private val fileSystem: FileSystem<*>,
    private val bufferField: Field,
) : WasiMemoryReader {
    override fun read(fd: Fd, strategy: ReadWriteStrategy, iovecs: IovecArray): ULong {
        val memoryByteBuffer = bufferField.get(memory) as? ByteBuffer
            ?: error("Can not get memory byte buffer")
        val bbufs = iovecs.toByteBuffers(memoryByteBuffer)
        return fileSystem.read(fd, bbufs, strategy)
    }

    private fun IovecArray.toByteBuffers(
        memoryBuffer: ByteBuffer,
    ): List<FileSystemByteBuffer> = List(iovecList.size) { iovecNo ->
        val ioVec = iovecList[iovecNo]
        check(memoryBuffer.hasArray()) { "MemoryBuffer without array" }
        FileSystemByteBuffer(
            memoryBuffer.array(),
            memoryBuffer.arrayOffset() + ioVec.buf.addr,
            ioVec.bufLen.value.toInt(),
        )
    }

    companion object {
        @Suppress("ReturnCount", "SwallowedException")
        fun create(
            memory: Memory,
            fileSystem: FileSystem<*>,
        ): ChicoryWasiMemoryReader? {
            try {
                val bufferField = Memory::class.java.getDeclaredField("buffer")
                if (!bufferField.trySetAccessible()) {
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
