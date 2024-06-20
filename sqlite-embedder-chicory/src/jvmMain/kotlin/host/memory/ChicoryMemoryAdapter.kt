/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.memory

import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import com.dylibso.chicory.runtime.Memory as ChicoryMemory

@Suppress("BLANK_LINE_BETWEEN_PROPERTIES")
internal class ChicoryMemoryAdapter(
    internal val wasmMemory: ChicoryMemory,
) : Memory {
    override fun readI8(addr: WasmPtr<*>): Byte {
        return wasmMemory.read(addr.addr)
    }

    override fun readI32(addr: WasmPtr<*>): Int {
        return wasmMemory.readI32(addr.addr).asInt()
    }

    override fun readI64(addr: WasmPtr<*>): Long {
        return wasmMemory.readI64(addr.addr).asLong()
    }

    override fun read(addr: WasmPtr<*>, destination: ByteArray, destinationOffset: Int, readBytes: Int) {
        val bytes = wasmMemory.readBytes(addr.addr, readBytes)
        bytes.copyInto(destination, destinationOffset)
    }

    override fun writeI8(addr: WasmPtr<*>, data: Byte) {
        wasmMemory.writeByte(addr.addr, data)
    }

    override fun writeI32(addr: WasmPtr<*>, data: Int) {
        wasmMemory.writeI32(addr.addr, data)
    }

    override fun writeI64(addr: WasmPtr<*>, data: Long) {
        wasmMemory.writeLong(addr.addr, data)
    }

    override fun write(addr: WasmPtr<*>, source: ByteArray, sourceOffset: Int, writeBytes: Int) {
        wasmMemory.write(addr.addr, source, sourceOffset, writeBytes)
    }
}
