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
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.SyscallReadlinkatFunctionHandle

internal class SyscallReadlinkat(
    host: EmbedderHost,
    private val memory: Memory,
) : EmscriptenHostFunctionHandle {
    private val handle = SyscallReadlinkatFunctionHandle(host)

    override fun apply(instance: Instance, vararg args: Value): Value? {
        val sizeOrErrno = handle.execute(
            memory,
            rawDirFd = args[0].asInt(),
            pathnamePtr = args[1].asWasmAddr(),
            buf = args[2].asWasmAddr(),
            bufSize = args[3].asInt(),
        )
        return Value.i32(sizeOrErrno.toLong())
    }
}
