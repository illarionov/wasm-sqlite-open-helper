/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory

import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.asInputStream
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr

internal class GraalvmMemoryRawSink(
    private val memoryProvider: () -> WasmMemory,
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

        val inputStream = source.asInputStream()
        val bytesWritten = memoryProvider().copyFromStream(null, inputStream, baseAddr.addr, byteCount.toInt())
        check(bytesWritten >= 0) {
            "End of the stream has been reached"
        }
        baseAddr = WasmPtr<Unit>(endAddrExclusive.toInt())
    }

    override fun flush() = Unit

    override fun close() {
        isClosed = true
    }
}
