/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MagicNumber")

package ru.pixnews.wasm.sqlite.open.helper.host.test.fixtures

import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.readTo
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.DefaultWasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.DefaultWasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.test.utils.KermitLogger

public open class TestMemory(
    public val size: Int = 1_048_576,
    public val fileSystem: FileSystem<*> = TestFileSystem(),
    public val logger: Logger = KermitLogger(),
) : Memory {
    public val bytes: ByteArray = ByteArray(size) { 0xdc.toByte() }
    public val memoryReader: DefaultWasiMemoryReader = DefaultWasiMemoryReader(this, fileSystem, logger)
    public val memoryWriter: DefaultWasiMemoryWriter = DefaultWasiMemoryWriter(this, fileSystem, logger)

    public fun fill(value: Byte) {
        bytes.fill(value)
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

    override fun read(fromAddr: WasmPtr<*>, toSink: RawSink, readBytes: Int) {
        val toSinkBuffered = toSink.buffered()
        toSinkBuffered.write(
            source = bytes,
            startIndex = fromAddr.addr,
            endIndex = fromAddr.addr + readBytes,
        )
        toSinkBuffered.emit()
    }

    override fun writeI8(addr: WasmPtr<*>, data: Byte) {
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

    override fun write(fromSource: RawSource, toAddr: WasmPtr<*>, writeBytes: Int) {
        val fromSourceBuffered = fromSource.buffered()
        fromSourceBuffered.readTo(
            sink = bytes,
            startIndex = toAddr.addr,
            endIndex = toAddr.addr + writeBytes,
        )
    }

    override fun toString(): String {
        return "TestMemory(size=$size)"
    }
}
