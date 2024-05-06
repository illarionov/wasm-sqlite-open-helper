/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.function

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.WasiHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function.FdWriteFdPWriteFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CioVec
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class FdWriteFdPwrite private constructor(
    private val memory: Memory,
    private val handle: FdWriteFdPWriteFunctionHandle,
) : WasiHostFunctionHandle {
    override fun apply(instance: Instance, vararg args: Value): Errno {
        val fd = Fd(args[0].asInt())
        val pCiov: WasmPtr<CioVec> = args[1].asWasmAddr()
        val cIovCnt = args[2].asInt()
        val pNum: WasmPtr<Int> = args[3].asWasmAddr()
        return handle.execute(memory, fd, pCiov, cIovCnt, pNum)
    }

    companion object {
        fun fdWrite(
            host: SqliteEmbedderHost,
            memory: Memory,
        ): WasiHostFunctionHandle = FdWriteFdPwrite(memory, FdWriteFdPWriteFunctionHandle.fdWrite(host))

        fun fdPwrite(
            host: SqliteEmbedderHost,
            memory: Memory,
        ): WasiHostFunctionHandle = FdWriteFdPwrite(memory, FdWriteFdPWriteFunctionHandle.fdPwrite(host))
    }
}
