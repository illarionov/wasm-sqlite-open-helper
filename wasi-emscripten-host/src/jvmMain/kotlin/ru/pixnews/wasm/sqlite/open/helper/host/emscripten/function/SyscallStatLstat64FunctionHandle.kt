/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readZeroTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.pack
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.memory.write

public class SyscallStatLstat64FunctionHandle private constructor(
    host: EmbedderHost,
    private val followSymlinks: Boolean = false,
    function: HostFunction,
) : HostFunctionHandle(function, host) {
    public fun execute(
        memory: Memory,
        pathnamePtr: WasmPtr<Byte>,
        dst: WasmPtr<StructStat>,
    ): Int {
        var path = ""
        try {
            path = memory.readZeroTerminatedString(pathnamePtr)
            val stat = host.fileSystem.stat(
                path = path,
                followSymlinks = followSymlinks,
            ).also {
                logger.v { "`$path`: $it" }
            }.pack()
            memory.write(dst, stat)
        } catch (e: SysException) {
            logger.v { "`$path`: error ${e.errNo}" }
            return -e.errNo.code
        }

        return 0
    }

    public companion object {
        public fun syscallLstat64(
            host: EmbedderHost,
        ): SyscallStatLstat64FunctionHandle = SyscallStatLstat64FunctionHandle(
            host,
            false,
            EmscriptenHostFunction.SYSCALL_LSTAT64,
        )

        public fun syscallStat64(
            host: EmbedderHost,
        ): SyscallStatLstat64FunctionHandle = SyscallStatLstat64FunctionHandle(
            host,
            true,
            EmscriptenHostFunction.SYSCALL_STAT64,
        )
    }
}
