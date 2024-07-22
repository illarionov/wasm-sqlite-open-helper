/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.memory

import io.github.charlietap.chasm.embedding.memory.writeMemory
import io.github.charlietap.chasm.executor.runtime.store.Address
import io.github.charlietap.chasm.executor.runtime.store.Store
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr

internal class ChasmMemoryRawSink(
    private val store: Store,
    private val memoryAddress: Address.Memory,
    private var baseAddr: WasmPtr<*>,
    private val toAddrExclusive: WasmPtr<*>,
) : RawSink {
    private var isClosed: Boolean = false

    override fun write(source: Buffer, byteCount: Long) {
        require(byteCount >= 0) { "byteCount is negative" }
        check(!isClosed) { "Stream is closed" }

        val endAddrExclusive = baseAddr.addr + byteCount
        require(endAddrExclusive <= toAddrExclusive.addr) {
            "Cannot write `$byteCount` bytes to memory range $baseAddr ..<$toAddrExclusive: out of boundary access"
        }

        for (addr in baseAddr.addr..<endAddrExclusive.toInt()) {
            val byte = source.readByte()
            writeMemory(store, memoryAddress, addr, byte)
        }
        baseAddr = WasmPtr<Unit>(endAddrExclusive.toInt())
    }

    override fun flush() {
        // no-op
    }

    override fun close() {
        isClosed = true
    }
}
