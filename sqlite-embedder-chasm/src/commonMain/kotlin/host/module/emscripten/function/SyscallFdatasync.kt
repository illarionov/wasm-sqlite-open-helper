/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function

import io.github.charlietap.chasm.executor.runtime.value.ExecutionValue
import io.github.charlietap.chasm.executor.runtime.value.NumberValue.I32
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asInt
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.EmscriptenHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function.FdSyncSyscallFdatasyncFunctionHandle

internal class SyscallFdatasync(
    host: EmbedderHost,
) : EmscriptenHostFunctionHandle {
    val handle = FdSyncSyscallFdatasyncFunctionHandle.syscallFdatasync(host)

    override fun invoke(args: List<ExecutionValue>): List<ExecutionValue> {
        return listOf(I32(handle.execute(Fd(args[0].asInt())).code))
    }
}
