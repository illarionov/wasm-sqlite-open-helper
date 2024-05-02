/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.ext.WasiEnvironmentFunc
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno

public class EnvironSizesGetFunctionHandle(
    host: SqliteEmbedderHost,
) : HostFunctionHandle(WasiHostFunction.ENVIRON_SIZES_GET, host) {
    public fun execute(
        memory: Memory,
        environCountAddr: WasmPtr<Int>,
        environSizeAddr: WasmPtr<Int>,
    ): Errno {
        return WasiEnvironmentFunc.environSizesGet(
            envProvider = host.systemEnvProvider,
            memory = memory,
            environCountAddr = environCountAddr,
            environSizeAddr = environSizeAddr,
        )
    }
}
