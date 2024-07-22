/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.memory

import com.dylibso.chicory.runtime.Memory
import com.dylibso.chicory.runtime.exceptions.WASMRuntimeException
import kotlinx.io.Buffer
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.MemoryRawSource

internal class ChicoryMemoryRawSource(
    private val wasmMemory: Memory,
    baseAddr: WasmPtr<*>,
    toAddrExclusive: WasmPtr<*>,
) : MemoryRawSource(baseAddr, toAddrExclusive) {
    override fun readBytesFromMemory(srcAddr: WasmPtr<*>, sink: Buffer, readBytes: Int) {
        try {
            val bytes = wasmMemory.readBytes(srcAddr.addr, readBytes)
            sink.write(bytes)
        } catch (oob: WASMRuntimeException) {
            throw IllegalStateException("Out of bounds memory access", oob)
        } finally {
            sink.emit()
        }
    }
}
