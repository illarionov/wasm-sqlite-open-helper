/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.memory

import io.github.charlietap.chasm.ChasmResult.Error
import io.github.charlietap.chasm.ChasmResult.Success
import io.github.charlietap.chasm.embedding.memory.readMemory
import io.github.charlietap.chasm.executor.runtime.store.Address
import io.github.charlietap.chasm.executor.runtime.store.Store
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import kotlin.concurrent.Volatile

internal class ChasmMemoryRawSource(
    private val store: Store,
    private val memoryAddress: Address.Memory,
    private val baseAddr: WasmPtr<*>,
) : RawSource {
    @Volatile
    private var isClosed: Boolean = false

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        require(byteCount >= 0) { "byteCount is negative" }
        check(!isClosed) { "Stream is closed" }

        val readBytes = byteCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        try {
            for (addr in baseAddr.addr..<baseAddr.addr + readBytes) {
                @Suppress("UseCheckOrError")
                when (val readResult = readMemory(store, memoryAddress, addr)) {
                    is Success -> sink.writeByte(readResult.result)
                    is Error -> throw IllegalStateException("Read from memory failed: ${readResult.error.error}")
                }
            }
        } finally {
            sink.emit()
        }
        return readBytes.toLong()
    }

    override fun close() {
        isClosed = true
    }
}
