/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MemberNameEqualsClassName")

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.EmscriptenHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.SyscallStatLstat64FunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory

internal fun syscallStat64(
    host: SqliteEmbedderHost,
    memory: Memory,
): EmscriptenHostFunctionHandle = SyscallStat64Lstat64(memory, SyscallStatLstat64FunctionHandle.syscallStat64(host))

internal fun syscallLstat64(
    host: SqliteEmbedderHost,
    memory: Memory,
): EmscriptenHostFunctionHandle = SyscallStat64Lstat64(memory, SyscallStatLstat64FunctionHandle.syscallLstat64(host))

internal class SyscallStat64Lstat64(
    private val memory: Memory,
    private val handle: SyscallStatLstat64FunctionHandle,
) : EmscriptenHostFunctionHandle {
    override fun apply(instance: Instance, vararg args: Value): Value? {
        val result = handle.execute(
            memory,
            args[0].asWasmAddr(),
            args[1].asWasmAddr(),
        )
        return Value.i32(result.toLong())
    }
}
