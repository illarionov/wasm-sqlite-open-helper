/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.EmscriptenHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.MunapJsFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysMmanMapFlags
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysMmanProt
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class MunmapJs(
    host: EmbedderHost,
    @Suppress("UNUSED_PARAMETER") memory: Memory,
) : EmscriptenHostFunctionHandle {
    private val handle = MunapJsFunctionHandle(host)

    @Suppress("MagicNumber")
    override fun apply(instance: Instance, vararg args: Value): Value? {
        val result: Int = handle.execute(
            args[0].asWasmAddr(),
            args[1].asInt(),
            SysMmanProt(args[2].asUInt().toUInt()),
            SysMmanMapFlags(args[3].asUInt().toUInt()),
            Fd(args[4].asInt()),
            args[5].asLong().toULong(),
        )
        return Value.i32(result.toLong())
    }
}
