/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function

import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.writeNullTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.host.base.plus
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.ext.WasiEnvironmentFunc.encodeEnvToWasi
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno

public class EnvironGetFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(WasiHostFunction.ENVIRON_GET, host) {
    public fun execute(
        memory: Memory,
        environPAddr: WasmPtr<Int>,
        environBufAddr: WasmPtr<Int>,
    ): Errno {
        var pp = environPAddr
        var bufP = environBufAddr
        host.systemEnvProvider.getSystemEnv()
            .entries
            .map { it.encodeEnvToWasi() }
            .forEach { envString ->
                memory.writeI32(pp, bufP.addr)
                pp += 4
                bufP += memory.writeNullTerminatedString(bufP, envString)
            }
        return Errno.SUCCESS
    }
}
