/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.chicory
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType
import com.dylibso.chicory.runtime.HostFunction as ChicoryHostFunction
import com.dylibso.chicory.runtime.WasmFunctionHandle as ChicoryWasmFunctionHandle

internal fun emscriptenEnvHostFunction(
    funcName: String,
    paramTypes: List<WasmValueType>,
    returnType: WasmValueType?,
    moduleName: String = ENV_MODULE_NAME,
    handle: EmscriptenHostFunction,
): ChicoryHostFunction = ChicoryHostFunction(
    HostFunctionAdapter(handle),
    moduleName,
    funcName,
    paramTypes.map(WasmValueType::chicory),
    returnType?.let { listOf(it.chicory) } ?: listOf(),
)

internal fun interface EmscriptenHostFunction {
    fun apply(instance: Instance, vararg args: Value): Value?
}

private class HostFunctionAdapter(
    private val delegate: EmscriptenHostFunction,
) : ChicoryWasmFunctionHandle {
    override fun apply(instance: Instance, vararg args: Value): Array<Value> {
        val result: Value? = delegate.apply(instance, args = args)
        return if (result != null) {
            arrayOf(result)
        } else {
            arrayOf()
        }
    }
}
