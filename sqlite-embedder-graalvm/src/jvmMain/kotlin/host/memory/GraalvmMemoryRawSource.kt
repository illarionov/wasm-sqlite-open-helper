/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory

import com.oracle.truffle.api.nodes.Node
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.asOutputStream
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.plus
import java.io.IOException

internal class GraalvmMemoryRawSource(
    private val memoryProvider: () -> WasmMemory,
    private var baseAddr: WasmPtr<*>,
    private val toAddrExclusive: WasmPtr<*>,
    private val node: Node?,
) : RawSource {
    private var isClosed: Boolean = false

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        require(byteCount >= 0) { "byteCount is negative" }
        check(!isClosed) { "Stream is closed" }

        val bytesLeft: Long = (toAddrExclusive.addr - baseAddr.addr).toLong()
        val readBytes = byteCount.coerceAtMost(bytesLeft).toInt()
        if (readBytes <= 0) {
            return -1
        }

        val outputStream = sink.asOutputStream()
        try {
            memoryProvider().copyToStream(node, outputStream, baseAddr.addr, readBytes)
        } catch (ioe: IOException) {
            throw IllegalStateException("Can not read from memory", ioe)
        } finally {
            outputStream.flush()
        }
        baseAddr += readBytes

        return readBytes.toLong()
    }

    override fun close() {
        isClosed = true
    }
}
