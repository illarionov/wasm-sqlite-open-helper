/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.chicory
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno

internal const val WASI_SNAPSHOT_PREVIEW1 = "wasi_snapshot_preview1"

internal fun wasiHostFunction(
    funcName: String,
    paramTypes: List<WasmValueType>,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
    handle: WasiHostFunction,
): HostFunction = HostFunction(
    WasiHostFunctionAdapter(handle),
    moduleName,
    funcName,
    paramTypes.map(WasmValueType::chicory),
    listOf(Errno.wasmValueType.chicory),
)

internal fun interface WasiHostFunction {
    fun apply(instance: Instance, vararg args: Value): Errno
}

private class WasiHostFunctionAdapter(
    private val delegate: WasiHostFunction,
) : WasmFunctionHandle {
    override fun apply(instance: Instance, vararg args: Value): Array<Value> {
        val result = delegate.apply(instance, args = args)
        return arrayOf(Value.i32(result.code.toLong()))
    }
}
