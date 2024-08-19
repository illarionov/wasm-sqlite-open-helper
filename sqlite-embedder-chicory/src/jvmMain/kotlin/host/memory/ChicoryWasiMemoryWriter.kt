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
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.DefaultWasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.FileSystemByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.WriteError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.WriteFd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CiovecArray
import java.lang.reflect.Field
import java.nio.ByteBuffer

internal class ChicoryWasiMemoryWriter private constructor(
    private val memory: Memory,
    private val fileSystem: FileSystem,
    private val bufferField: Field,
) : WasiMemoryWriter {
    override fun write(fd: Fd, strategy: ReadWriteStrategy, cioVecs: CiovecArray): Either<WriteError, ULong> {
        val memoryByteBuffer = bufferField.get(memory) as? ByteBuffer
            ?: error("Can not get memory byte buffer")
        val bbufs = cioVecs.toByteBuffers(memoryByteBuffer)
        return fileSystem.execute(WriteFd, WriteFd(fd, bbufs, strategy))
    }

    private fun CiovecArray.toByteBuffers(
        memoryBuffer: ByteBuffer,
    ): List<FileSystemByteBuffer> = List(ciovecList.size) {
        val ioVec = ciovecList[it]
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
        ): WasiMemoryWriter = if (isJvmOrAndroidMinApi34()) {
            tryCreate(memory.wasmMemory, fileSystem)
        } else {
            null
        } ?: DefaultWasiMemoryWriter(memory, fileSystem, logger)

        @Suppress("ReturnCount", "SwallowedException")
        fun tryCreate(
            memory: Memory,
            fileSystem: FileSystem,
        ): ChicoryWasiMemoryWriter? {
            try {
                val bufferField = Memory::class.java.getDeclaredField("buffer")
                if (!bufferField.trySetAccessibleCompat()) {
                    return null
                }
                if (bufferField.get(memory) !is ByteBuffer) {
                    return null
                }
                return ChicoryWasiMemoryWriter(memory, fileSystem, bufferField)
            } catch (nsfe: NoSuchFileException) {
                return null
            } catch (se: SecurityException) {
                return null
            }
        }
    }
}
