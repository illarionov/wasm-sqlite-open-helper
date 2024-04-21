/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.emscriptenEnvHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.F64

internal fun emscriptenGetNow(
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "emscripten_get_now",
    paramTypes = listOf(),
    returnType = F64,
    moduleName = moduleName,
) { _, _ ->
    @Suppress("MagicNumber")
    val ts = System.nanoTime() / 1_000_000.0
    Value.fromDouble(ts)
}
