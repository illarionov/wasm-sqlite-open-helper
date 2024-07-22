/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.test.fixtures

import kotlinx.io.Buffer
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.MemoryRawSource

public class TestMemoryRawSource(
    private val memory: TestMemory,
    baseAddr: WasmPtr<*>,
    toAddrExclusive: WasmPtr<*>,
) : MemoryRawSource(baseAddr, toAddrExclusive) {
    override fun readBytesFromMemory(srcAddr: WasmPtr<*>, sink: Buffer, readBytes: Int) {
        sink.write(
            source = memory.bytes,
            startIndex = srcAddr.addr,
            endIndex = srcAddr.addr + readBytes,
        )
        sink.emit()
    }
}
