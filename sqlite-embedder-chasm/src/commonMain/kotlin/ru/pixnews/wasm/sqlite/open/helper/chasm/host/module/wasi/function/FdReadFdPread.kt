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
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function.FdReadFdPreadFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Iovec

internal class FdReadFdPread private constructor(
    private val memory: Memory,
    private val handle: FdReadFdPreadFunctionHandle,
) : WasiHostFunctionHandle {
    override fun invoke(args: List<ExecutionValue>): Errno {
        val fd = Fd(args[0].asInt())
        val pIov: WasmPtr<Iovec> = args[1].asWasmAddr()
        val iovCnt = args[2].asInt()
        val pNum: WasmPtr<Int> = args[3].asWasmAddr()
        return handle.execute(memory, fd, pIov, iovCnt, pNum)
    }

    companion object {
        fun fdRead(
            host: EmbedderHost,
            memory: Memory,
        ): WasiHostFunctionHandle = FdReadFdPread(memory, FdReadFdPreadFunctionHandle.fdRead(host))

        fun fdPread(
            host: EmbedderHost,
            memory: Memory,
        ): WasiHostFunctionHandle = FdReadFdPread(memory, FdReadFdPreadFunctionHandle.fdPread(host))
    }
}
