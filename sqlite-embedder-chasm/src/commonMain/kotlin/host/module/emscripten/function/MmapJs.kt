/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function

import io.github.charlietap.chasm.executor.runtime.value.ExecutionValue
import io.github.charlietap.chasm.executor.runtime.value.NumberValue.I32
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asInt
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asUInt
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asULong
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.EmscriptenHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.MmapJsFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysMmanMapFlags
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysMmanProt

internal class MmapJs(
    host: EmbedderHost,
) : EmscriptenHostFunctionHandle {
    private val handle = MmapJsFunctionHandle(host)

    @Suppress("MagicNumber")
    override fun invoke(args: List<ExecutionValue>): List<ExecutionValue> {
        val result: Int = handle.execute(
            args[0].asInt(),
            SysMmanProt(args[1].asUInt()),
            SysMmanMapFlags(args[2].asUInt()),
            Fd(args[3].asInt()),
            args[4].asULong(),
            args[5].asWasmAddr(),
            args[6].asWasmAddr(),
        )
        return listOf(I32(result))
    }
}
