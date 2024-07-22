/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.test.fixtures

import kotlinx.io.Buffer
import kotlinx.io.RawSource
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import kotlin.concurrent.Volatile

public class TestMemoryRawSource(
    private val memory: TestMemory,
    public val baseAddr: WasmPtr<*>,
) : RawSource {
    @Volatile
    private var isClosed: Boolean = false

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        require(byteCount >= 0) { "byteCount is negative" }
        check(!isClosed) { "Stream is closed" }

        val readBytes = byteCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

        sink.write(
            source = memory.bytes,
            startIndex = baseAddr.addr,
            endIndex = baseAddr.addr + readBytes,
        )
        sink.emit()

        return readBytes.toLong()
    }

    override fun close() {
        isClosed = true
    }
}
