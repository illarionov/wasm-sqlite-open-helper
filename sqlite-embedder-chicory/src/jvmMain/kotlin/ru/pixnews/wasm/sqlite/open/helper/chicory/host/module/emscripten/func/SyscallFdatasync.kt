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
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function.FdSyncSyscallFdatasyncFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class SyscallFdatasync(
    host: SqliteEmbedderHost,
    @Suppress("UNUSED_PARAMETER") memory: Memory,
) : EmscriptenHostFunctionHandle {
    val handle = FdSyncSyscallFdatasyncFunctionHandle.syscallFdatasync(host)
    override fun apply(instance: Instance, vararg args: Value): Value? {
        return Value.i32(handle.execute(Fd(args[0].asInt())).code.toLong())
    }
}
