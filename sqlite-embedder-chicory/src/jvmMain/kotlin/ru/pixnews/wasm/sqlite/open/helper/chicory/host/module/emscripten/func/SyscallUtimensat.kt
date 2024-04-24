/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.emscriptenEnvHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem

internal fun syscallUtimensat(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "__syscall_utimensat",
    paramTypes = listOf(
        I32,
        I32,
    ),
    returnType = I32,
    moduleName = moduleName,
    handle = SyscallUtimensat(filesystem),
)

private class SyscallUtimensat(
    private val filesystem: FileSystem,
) : EmscriptenHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Value {
        TODO("Not yet implemented")
    }
}
