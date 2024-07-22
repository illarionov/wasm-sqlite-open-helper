/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.base.memory

import kotlinx.io.Buffer
import kotlinx.io.RawSink
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr

@InternalWasmSqliteHelperApi
public abstract class MemoryRawSink(
    protected var baseAddr: WasmPtr<*>,
    protected val toAddrExclusive: WasmPtr<*>,
) : RawSink {
    private var isClosed: Boolean = false
    override fun close() {
        isClosed = true
    }

    override fun write(source: Buffer, byteCount: Long) {
        require(byteCount >= 0) { "byteCount is negative" }
        check(!isClosed) { "Stream is closed" }

        val endAddrExclusive = getEndAddressOrThrow(byteCount)

        writeBytesToMemory(source, baseAddr, byteCount)
        baseAddr = WasmPtr<Unit>(endAddrExclusive.toInt())
    }

    protected abstract fun writeBytesToMemory(
        source: Buffer,
        toAddr: WasmPtr<*>,
        byteCount: Long,
    )

    override fun flush(): Unit = Unit

    protected fun getEndAddressOrThrow(
        byteCount: Long,
    ): Long {
        val endAddrExclusive = baseAddr.addr + byteCount
        require(endAddrExclusive <= toAddrExclusive.addr) {
            "Cannot write `$byteCount` bytes to memory range $baseAddr ..<$toAddrExclusive: out of boundary access"
        }
        return endAddrExclusive
    }
}
