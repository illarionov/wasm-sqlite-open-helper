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
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.MemoryRawSink

internal class ChasmMemoryRawSink(
    private val store: Store,
    private val memoryAddress: Address.Memory,
    baseAddr: WasmPtr<*>,
    toAddrExclusive: WasmPtr<*>,
) : MemoryRawSink(baseAddr, toAddrExclusive) {
    override fun writeBytesToMemory(source: Buffer, toAddr: WasmPtr<*>, byteCount: Long) {
        for (addr in baseAddr.addr..<(baseAddr.addr + byteCount).toInt()) {
            val byte = source.readByte()
            writeMemory(store, memoryAddress, addr, byte)
        }
    }
}
