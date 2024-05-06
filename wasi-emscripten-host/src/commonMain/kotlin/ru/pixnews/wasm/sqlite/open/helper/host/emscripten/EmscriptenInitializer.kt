/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.EmbedderMemory
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmFunctionBinding

@InternalWasmSqliteHelperApi
@Suppress("MagicNumber")
public class EmscriptenInitializer(
    private val memory: EmbedderMemory,
    private val emscriptenStackInit: WasmFunctionBinding,
    private val emscriptenStackGetEnd: WasmFunctionBinding,
) {
    public fun init() {
        emscriptenStackInit.executeVoid()
        writeStackCookie()
        checkStackCookie()
    }

    private fun writeStackCookie() {
        var max = emscriptenStackGetEnd.executeForInt()
        check(max.and(0x03) == 0)

        if (max == 0) {
            max = 4
        }

        memory.writeI32(WasmPtr<Unit>(max), 0x0213_5467)
        memory.writeI32(WasmPtr<Unit>(max + 4), 0x89BA_CDFE_U.toInt())
        memory.writeI32(WasmPtr<Unit>(0), 1_668_509_029)
    }

    private fun checkStackCookie() {
        var max = emscriptenStackGetEnd.executeForInt()
        check(max.and(0x03) == 0)

        if (max == 0) {
            max = 4
        }

        val cookie1 = memory.readI32(WasmPtr<Unit>(max))
        val cookie2 = memory.readI32(WasmPtr<Unit>(max + 4))

        check(cookie1 == 0x0213_5467 && cookie2 == 0x89BA_CDFE_U.toInt()) {
            "Stack overflow! Stack cookie has been overwritten at ${max.toString(16)}, expected hex dwords " +
                    "0x89BACDFE and 0x2135467, but received ${cookie2.toString(16)} ${cookie2.toString(16)}"
        }
    }
}
