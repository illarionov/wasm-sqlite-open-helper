/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.memory

import com.dylibso.chicory.runtime.Memory
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.MemoryRawSink

internal class ChicoryMemoryRawSink(
    private val wasmMemory: Memory,
    baseAddr: WasmPtr<*>,
    toAddrExclusive: WasmPtr<*>,
) : MemoryRawSink(baseAddr, toAddrExclusive) {
    override fun writeBytesToMemory(source: Buffer, toAddr: WasmPtr<*>, byteCount: Long) {
        val data = source.readByteArray(byteCount.toInt())
        wasmMemory.write(baseAddr.addr, data)
    }
}
