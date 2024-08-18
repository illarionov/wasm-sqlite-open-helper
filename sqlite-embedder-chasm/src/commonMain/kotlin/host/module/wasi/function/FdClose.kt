/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi.function

import io.github.charlietap.chasm.executor.runtime.value.ExecutionValue
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asInt
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi.WasiHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function.FdCloseFunctionHandle

internal class FdClose(
    host: EmbedderHost,
) : WasiHostFunctionHandle {
    private val handle = FdCloseFunctionHandle(host)

    override fun invoke(args: List<ExecutionValue>): Errno {
        return handle.execute(Fd(args[0].asInt()))
    }
}
