/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MemberNameEqualsClassName")

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function

import io.github.charlietap.chasm.executor.runtime.value.ExecutionValue
import io.github.charlietap.chasm.executor.runtime.value.NumberValue.I32
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.EmscriptenHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.SyscallStatLstat64FunctionHandle

internal fun syscallStat64(
    host: EmbedderHost,
    memory: Memory,
): EmscriptenHostFunctionHandle = SyscallStat64Lstat64(memory, SyscallStatLstat64FunctionHandle.syscallStat64(host))

internal fun syscallLstat64(
    host: EmbedderHost,
    memory: Memory,
): EmscriptenHostFunctionHandle = SyscallStat64Lstat64(memory, SyscallStatLstat64FunctionHandle.syscallLstat64(host))

internal class SyscallStat64Lstat64(
    private val memory: Memory,
    private val handle: SyscallStatLstat64FunctionHandle,
) : EmscriptenHostFunctionHandle {
    override fun invoke(args: List<ExecutionValue>): List<ExecutionValue> {
        val result = handle.execute(
            memory,
            args[0].asWasmAddr(),
            args[1].asWasmAddr(),
        )
        return listOf(I32(result))
    }
}
