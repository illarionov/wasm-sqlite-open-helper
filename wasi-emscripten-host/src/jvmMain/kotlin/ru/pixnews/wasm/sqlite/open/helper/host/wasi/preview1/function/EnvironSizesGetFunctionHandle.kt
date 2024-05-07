/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.encodedNullTerminatedStringLength
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.ext.WasiEnvironmentFunc.encodeEnvToWasi
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.SUCCESS

public class EnvironSizesGetFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(WasiHostFunction.ENVIRON_SIZES_GET, host) {
    public fun execute(
        memory: Memory,
        environCountAddr: WasmPtr<Int>,
        environSizeAddr: WasmPtr<Int>,
    ): Errno {
        val env = host.systemEnvProvider()
        val count = env.size
        val dataLength = env.entries.sumOf { it.encodeEnvToWasi().encodedNullTerminatedStringLength() }
        memory.writeI32(
            addr = environCountAddr,
            data = count,
        )
        memory.writeI32(
            addr = environSizeAddr,
            data = dataLength,
        )
        return SUCCESS
    }
}
