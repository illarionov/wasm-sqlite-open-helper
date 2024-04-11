/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory

import com.oracle.truffle.api.nodes.Node
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.memory.WasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.memory.WasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.IovecArray
import java.io.ByteArrayOutputStream

internal class WasmHostMemoryImpl(
    val memory: WasmMemory,
    internal val node: Node?,
    logger: Logger,
) : Memory {
    private val memoryReader: WasiMemoryReader = GraalInputStreamWasiMemoryReader(this, logger)
    private val memoryWriter: WasiMemoryWriter = GraalOutputStreamWasiMemoryWriter(this, logger)

    override fun readI8(addr: WasmPtr<*>): Byte {
        return memory.load_i32_8u(node, addr.addr.toLong()).toByte()
    }

    override fun readI32(addr: WasmPtr<*>): Int {
        return memory.load_i32(node, addr.addr.toLong())
    }

    override fun readBytes(addr: WasmPtr<*>, length: Int): ByteArray {
        val bous = ByteArrayOutputStream(length)
        memory.copyToStream(node, bous, addr.addr, length)
        return bous.toByteArray()
    }

    override fun writeByte(addr: WasmPtr<*>, data: Byte) {
        memory.store_i32_8(node, addr.addr.toLong(), data)
    }

    override fun writeI32(addr: WasmPtr<*>, data: Int) {
        memory.store_i32(node, addr.addr.toLong(), data)
    }

    override fun writeI64(addr: WasmPtr<*>, data: Long) {
        memory.store_i64(node, addr.addr.toLong(), data)
    }

    override fun write(addr: WasmPtr<*>, data: ByteArray, offset: Int, size: Int) {
        memory.initialize(data, offset, addr.addr.toLong(), size)
    }

    override fun readFromChannel(
        channel: FdChannel,
        strategy: ReadWriteStrategy,
        iovecs: IovecArray,
    ): ULong = memoryReader.read(channel, strategy, iovecs)

    override fun writeToChannel(
        channel: FdChannel,
        strategy: ReadWriteStrategy,
        cioVecs: CiovecArray,
    ): ULong = memoryWriter.write(channel, strategy, cioVecs)

    fun readNullTerminatedString(
        offset: WasmPtr<Byte>,
    ): String = memory.readString(offset.addr, null)
}
