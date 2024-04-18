/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.ext

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.plus
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.encodedNullTerminatedStringLength
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.writeZeroTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno

public object WasiEnvironmentFunc {
    public fun environSizesGet(
        memory: Memory,
        environCountAddr: WasmPtr<Int>,
        environSizeAddr: WasmPtr<Int>,
        envProvider: () -> Map<String, String>,
    ): Errno {
        val env = envProvider()
        val count = env.size
        val dataLength = env.entries.sumOf { it.encodeEnvToWasi().encodedNullTerminatedStringLength() }

        memory.writeI32(environCountAddr, count)
        memory.writeI32(environSizeAddr, dataLength)
        return Errno.SUCCESS
    }

    public fun environGet(
        memory: Memory,
        environPAddr: WasmPtr<Int>,
        environBufAddr: WasmPtr<Int>,
        envProvider: () -> Map<String, String>,
    ): Errno {
        var pp = environPAddr
        var bufP = environBufAddr

        envProvider()
            .entries
            .map { it.encodeEnvToWasi() }
            .forEach { envString ->
                memory.writeI32(pp, bufP.addr)
                pp += 4
                bufP += memory.writeZeroTerminatedString(bufP, envString)
            }

        return Errno.SUCCESS
    }

    // TODO: sanitize `=`?
    private fun Map.Entry<String, String>.encodeEnvToWasi(): String = "$key=$value"
}
