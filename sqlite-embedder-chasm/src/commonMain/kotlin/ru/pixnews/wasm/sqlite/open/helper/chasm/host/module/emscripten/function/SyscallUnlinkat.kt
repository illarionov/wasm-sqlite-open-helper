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
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.EmscriptenHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.SyscallUnlinkatFunctionHandle

internal class SyscallUnlinkat(
    host: EmbedderHost,
    private val memory: Memory,
) : EmscriptenHostFunctionHandle {
    private val handle = SyscallUnlinkatFunctionHandle(host)

    override fun invoke(args: List<ExecutionValue>): List<ExecutionValue> {
        val result = handle.execute(
            memory,
            args[0].asInt(),
            args[1].asWasmAddr(),
            args[2].asUInt(),
        )
        return listOf(I32(result))
    }
}
