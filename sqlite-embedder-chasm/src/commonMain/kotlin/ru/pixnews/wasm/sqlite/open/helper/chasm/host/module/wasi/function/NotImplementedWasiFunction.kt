/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi.function

import io.github.charlietap.chasm.executor.runtime.value.ExecutionValue
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi.WasiHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno

internal class NotImplementedWasiFunction(
    private val function: HostFunction,
) : WasiHostFunctionHandle {
    override fun invoke(args: List<ExecutionValue>): Errno {
        error("Function `$function` not implemented")
    }
}
