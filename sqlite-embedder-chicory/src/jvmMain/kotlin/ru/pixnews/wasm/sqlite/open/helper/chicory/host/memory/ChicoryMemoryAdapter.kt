/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.memory

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.DefaultWasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.DefaultWasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.IovecArray
import com.dylibso.chicory.runtime.Memory as ChicoryMemory

@Suppress("BLANK_LINE_BETWEEN_PROPERTIES")
internal class ChicoryMemoryAdapter(
    private val wasmMemory: ChicoryMemory,
    fileSystem: FileSystem<*>,
    logger: Logger,
) : Memory {
    private val memoryReader: WasiMemoryReader = if (isJvmOrAndroidMinApi34()) {
        ChicoryWasiMemoryReader.create(wasmMemory, fileSystem)
    } else {
        null
    } ?: DefaultWasiMemoryReader(this, fileSystem, logger)

    private val memoryWriter: WasiMemoryWriter = if (isJvmOrAndroidMinApi34()) {
        ChicoryWasiMemoryWriter.create(wasmMemory, fileSystem)
    } else {
        null
    } ?: DefaultWasiMemoryWriter(this, fileSystem, logger)

    override fun readI8(addr: WasmPtr<*>): Byte {
        return wasmMemory.read(addr.addr)
    }

    override fun readI32(addr: WasmPtr<*>): Int {
        return wasmMemory.readI32(addr.addr).asInt()
    }

    override fun readI64(addr: WasmPtr<*>): Long {
        return wasmMemory.readI64(addr.addr).asLong()
    }

    override fun readBytes(addr: WasmPtr<*>, length: Int): ByteArray {
        return wasmMemory.readBytes(addr.addr, length)
    }

    override fun writeByte(addr: WasmPtr<*>, data: Byte) {
        wasmMemory.writeByte(addr.addr, data)
    }

    override fun writeI32(addr: WasmPtr<*>, data: Int) {
        wasmMemory.writeI32(addr.addr, data)
    }

    override fun writeI64(addr: WasmPtr<*>, data: Long) {
        wasmMemory.writeLong(addr.addr, data)
    }

    override fun write(addr: WasmPtr<*>, data: ByteArray, offset: Int, size: Int) {
        wasmMemory.write(addr.addr, data, offset, size)
    }

    override fun readFromChannel(
        fd: Fd,
        strategy: ReadWriteStrategy,
        iovecs: IovecArray,
    ): ULong = memoryReader.read(fd, strategy, iovecs)

    override fun writeToChannel(
        fd: Fd,
        strategy: ReadWriteStrategy,
        cioVecs: CiovecArray,
    ): ULong = memoryWriter.write(fd, strategy, cioVecs)

    private companion object {
        @Suppress("PrivateApi", "MagicNumber", "TooGenericExceptionCaught", "SwallowedException")
        fun isJvmOrAndroidMinApi34(): Boolean {
            try {
                val sdkIntField = Class.forName("android.os.Build").getDeclaredField("SDK_INT")
                val version = sdkIntField.get(null) as? Int ?: 0
                return version >= 34
            } catch (ex: Exception) {
                // is JVM
                return true
            }
        }
    }
}
