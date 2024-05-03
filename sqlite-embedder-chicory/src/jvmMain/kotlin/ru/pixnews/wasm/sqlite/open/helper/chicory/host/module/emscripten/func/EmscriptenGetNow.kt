/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.EmscriptenHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.EmscriptenGetNowFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory

internal class EmscriptenGetNow(
    host: SqliteEmbedderHost,
    @Suppress("UnusedPrivateProperty") private val memory: Memory,
) : EmscriptenHostFunctionHandle {
    private val handle = EmscriptenGetNowFunctionHandle(host)

    override fun apply(instance: Instance, vararg args: Value): Value {
        return Value.fromDouble(handle.execute())
    }
}
