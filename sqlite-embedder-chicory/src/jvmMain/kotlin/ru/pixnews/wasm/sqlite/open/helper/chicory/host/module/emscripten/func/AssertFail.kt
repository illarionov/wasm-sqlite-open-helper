/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func

import com.dylibso.chicory.runtime.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.emscriptenEnvHostFunction
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readNullableZeroTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.AssertionFailedException
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.pointer
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.WasiValueTypes.U8

internal fun assertFail(
    memory: Memory,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "__assert_fail",
    paramTypes = listOf(
        U8.pointer, // pCondition
        U8.pointer, // filename
        I32, // line
        U8.pointer, // func
    ),
    returnType = null,
    moduleName = moduleName,
) { _, params ->
    throw AssertionFailedException(
        condition = memory.readNullableZeroTerminatedString(params[0].asWasmAddr()),
        filename = memory.readNullableZeroTerminatedString(params[1].asWasmAddr()),
        line = params[2].asInt(),
        func = memory.readNullableZeroTerminatedString(params[3].asWasmAddr()),
    )
}
