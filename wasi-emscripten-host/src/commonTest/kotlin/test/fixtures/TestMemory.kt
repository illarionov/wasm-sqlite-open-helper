/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.test.fixtures

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.DefaultWasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.DefaultWasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.IovecArray
import ru.pixnews.wasm.sqlite.test.utils.KermitLogger

open class TestMemory(
    val size: Int = 1_048_576,
    val fileSystem: FileSystem<*> = TestFileSystem(),
    val logger: Logger = KermitLogger(),
) : Memory {
    val bytes = ByteArray(size) { 0xdc.toByte() }
    private val memoryReader = DefaultWasiMemoryReader(this, fileSystem, logger)
    private val memoryWriter = DefaultWasiMemoryWriter(this, fileSystem, logger)

    fun fill(value: Byte) {
        bytes.fill(value)
    }

    override fun readFromChannel(fd: Fd, strategy: ReadWriteStrategy, iovecs: IovecArray): ULong {
        return memoryReader.read(fd, strategy, iovecs)
    }

    override fun writeToChannel(fd: Fd, strategy: ReadWriteStrategy, cioVecs: CiovecArray): ULong {
        return memoryWriter.write(fd, strategy, cioVecs)
    }

    override fun readI8(addr: WasmPtr<*>): Byte {
        return bytes[addr.addr]
    }

    override fun readI32(addr: WasmPtr<*>): Int = (bytes[addr.addr].toInt() and 0xff) or
            (bytes[addr.addr + 1].toInt() and 0xff shl 8) or
            (bytes[addr.addr + 2].toInt() and 0xff shl 16) or
            (bytes[addr.addr + 3].toInt() and 0xff shl 24)

    override fun readI64(addr: WasmPtr<*>): Long = (bytes[addr.addr].toLong() and 0xffL) or
            (bytes[addr.addr + 1].toLong() and 0xffL shl 8) or
            (bytes[addr.addr + 2].toLong() and 0xffL shl 16) or
            (bytes[addr.addr + 3].toLong() and 0xffL shl 24) or
            (bytes[addr.addr + 4].toLong() and 0xffL shl 32) or
            (bytes[addr.addr + 5].toLong() and 0xffL shl 40) or
            (bytes[addr.addr + 6].toLong() and 0xffL shl 48) or
            (bytes[addr.addr + 7].toLong() and 0xffL shl 56)

    override fun readBytes(addr: WasmPtr<*>, length: Int): ByteArray = ByteArray(length) {
        bytes[addr.addr + it]
    }

    override fun writeByte(addr: WasmPtr<*>, data: Byte) {
        bytes[addr.addr] = data
    }

    override fun writeI32(addr: WasmPtr<*>, data: Int) {
        bytes[addr.addr] = (data and 0xff).toByte()
        bytes[addr.addr + 1] = (data ushr 8 and 0xff).toByte()
        bytes[addr.addr + 2] = (data ushr 16 and 0xff).toByte()
        bytes[addr.addr + 3] = (data ushr 24 and 0xff).toByte()
    }

    override fun writeI64(addr: WasmPtr<*>, data: Long) {
        bytes[addr.addr] = (data and 0xffL).toByte()
        bytes[addr.addr + 1] = (data ushr 8 and 0xff).toByte()
        bytes[addr.addr + 2] = (data ushr 16 and 0xff).toByte()
        bytes[addr.addr + 3] = (data ushr 24 and 0xff).toByte()
        bytes[addr.addr + 4] = (data ushr 32 and 0xff).toByte()
        bytes[addr.addr + 5] = (data ushr 40 and 0xff).toByte()
        bytes[addr.addr + 6] = (data ushr 48 and 0xff).toByte()
        bytes[addr.addr + 7] = (data ushr 56 and 0xff).toByte()
    }

    override fun write(addr: WasmPtr<*>, data: ByteArray, offset: Int, size: Int) {
        for (i in 0 until size) {
            bytes[addr.addr + i] = data[offset + i]
        }
    }

    override fun toString(): String {
        return "TestMemory(size=$size)"
    }
}
