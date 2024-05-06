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
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.AssertFailFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory

internal class AssertFail(
    @Suppress("UnusedPrivateProperty") host: SqliteEmbedderHost,
    private val memory: Memory,
) : EmscriptenHostFunctionHandle {
    private val handle = AssertFailFunctionHandle(host)

    override fun apply(instance: Instance, vararg args: Value): Value? {
        handle.execute(
            memory,
            args[0].asWasmAddr(),
            args[1].asWasmAddr(),
            args[2].asInt(),
            args[3].asWasmAddr(),
        )
    }
}
