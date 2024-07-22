/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory

import kotlinx.io.Buffer
import kotlinx.io.asInputStream
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.MemoryRawSink

internal class GraalvmMemoryRawSink(
    private val memoryProvider: () -> WasmMemory,
    baseAddr: WasmPtr<*>,
    toAddrExclusive: WasmPtr<*>,
) : MemoryRawSink(baseAddr, toAddrExclusive) {
    override fun writeBytesToMemory(source: Buffer, toAddr: WasmPtr<*>, byteCount: Long) {
        val inputStream = source.asInputStream()
        val bytesWritten = memoryProvider().copyFromStream(null, inputStream, toAddr.addr, byteCount.toInt())
        check(bytesWritten >= 0) {
            "End of the stream has been reached"
        }
    }
}
