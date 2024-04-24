/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.memory

import com.dylibso.chicory.runtime.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.sqlite.open.helper.host.memory.WasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CiovecArray
import java.lang.reflect.Field
import java.nio.ByteBuffer

internal class ChicoryWasiMemoryWriter private constructor(
    private val memory: Memory,
    private val bufferField: Field,
) : WasiMemoryWriter {
    override fun write(channel: FdChannel, strategy: ReadWriteStrategy, cioVecs: CiovecArray): ULong {
        val memoryByteBuffer = bufferField.get(memory) as? ByteBuffer
            ?: error("Can not get memory byte buffer")

        val bbufs = cioVecs.toByteBuffers(memoryByteBuffer)
        return channel.fileSystem.write(channel, bbufs, strategy)
    }

    private fun CiovecArray.toByteBuffers(
        memoryBuffer: ByteBuffer,
    ): Array<ByteBuffer> = Array(ciovecList.size) {
        val ioVec = ciovecList[it]
        memoryBuffer.slice(
            ioVec.buf.addr,
            ioVec.bufLen.value.toInt(),
        )
    }

    companion object {
        @Suppress("ReturnCount", "SwallowedException")
        fun create(
            memory: Memory,
        ): ChicoryWasiMemoryWriter? {
            try {
                val bufferField = Memory::class.java.getDeclaredField("buffer")
                if (!bufferField.trySetAccessible()) {
                    return null
                }
                if (bufferField.get(memory) !is ByteBuffer) {
                    return null
                }
                return ChicoryWasiMemoryWriter(memory, bufferField)
            } catch (nsfe: NoSuchFileException) {
                return null
            } catch (se: SecurityException) {
                return null
            }
        }
    }
}
