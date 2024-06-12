/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function

import io.github.charlietap.chasm.executor.runtime.value.ExecutionValue
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.EmscriptenHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.EmscriptenAsmConstAsyncOnMainThreadFunctionHandle

internal class EmscriptenAsmConstAsyncOnMainThread(host: EmbedderHost) : EmscriptenHostFunctionHandle {
    private val handle = EmscriptenAsmConstAsyncOnMainThreadFunctionHandle(host)

    override fun invoke(args: List<ExecutionValue>): List<ExecutionValue> {
        handle.execute(
            args[0].asWasmAddr(),
            args[1].asWasmAddr(),
            args[2].asWasmAddr(),
        )
        return emptyList()
    }
}
