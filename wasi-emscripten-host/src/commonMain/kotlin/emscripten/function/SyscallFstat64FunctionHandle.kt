/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import kotlinx.io.buffered
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.sinkWithMaxSize
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.ext.negativeErrnoCode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.StatFd
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.STRUCT_SIZE_PACKED_SIZE
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.packTo
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

public class SyscallFstat64FunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.SYSCALL_FSTAT64, host) {
    public fun execute(
        memory: Memory,
        fd: Fd,
        dst: WasmPtr<StructStat>,
    ): Int = host.fileSystem.execute(StatFd, StatFd(fd))
        .map { stat: StructStat ->
            memory.sinkWithMaxSize(dst, STRUCT_SIZE_PACKED_SIZE).buffered().use {
                stat.packTo(it)
            }
        }.negativeErrnoCode()
}
