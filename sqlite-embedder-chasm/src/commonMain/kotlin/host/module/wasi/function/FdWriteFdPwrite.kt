/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi.function

import io.github.charlietap.chasm.executor.runtime.value.ExecutionValue
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asInt
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi.WasiHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function.FdWriteFdPWriteFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CioVec
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class FdWriteFdPwrite private constructor(
    private val memory: Memory,
    private val wasiMemoryWriter: WasiMemoryWriter,
    private val handle: FdWriteFdPWriteFunctionHandle,
) : WasiHostFunctionHandle {
    override fun invoke(args: List<ExecutionValue>): Errno {
        val fd = Fd(args[0].asInt())
        val pCiov: WasmPtr<CioVec> = args[1].asWasmAddr()
        val cIovCnt = args[2].asInt()
        val pNum: WasmPtr<Int> = args[3].asWasmAddr()
        return handle.execute(memory, wasiMemoryWriter, fd, pCiov, cIovCnt, pNum)
    }

    companion object {
        fun fdWrite(
            host: EmbedderHost,
            memory: Memory,
            wasiMemoryWriter: WasiMemoryWriter,
        ): WasiHostFunctionHandle = FdWriteFdPwrite(
            memory,
            wasiMemoryWriter,
            FdWriteFdPWriteFunctionHandle.fdWrite(host),
        )

        fun fdPwrite(
            host: EmbedderHost,
            memory: Memory,
            wasiMemoryWriter: WasiMemoryWriter,
        ): WasiHostFunctionHandle = FdWriteFdPwrite(
            memory,
            wasiMemoryWriter,
            FdWriteFdPWriteFunctionHandle.fdPwrite(host),
        )
    }
}
