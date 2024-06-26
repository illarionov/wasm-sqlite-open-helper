/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MemberNameEqualsClassName")

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.function

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.WasiHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function.FdSeekFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class FdSeek(
    host: EmbedderHost,
    private val memory: Memory,
) : WasiHostFunctionHandle {
    private val handle = FdSeekFunctionHandle(host)

    override fun apply(instance: Instance, vararg args: Value): Errno {
        val fd = Fd(args[0].asInt())
        val offset = args[1].asLong()
        val whenceInt = args[2].asInt()
        val pNewOffset: WasmPtr<Long> = args[3].asWasmAddr()
        return handle.execute(memory, fd, offset, whenceInt, pNewOffset)
    }
}
