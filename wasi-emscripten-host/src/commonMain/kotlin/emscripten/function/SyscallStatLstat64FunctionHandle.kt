/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import kotlinx.io.buffered
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.readNullTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.sinkWithMaxSize
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.ext.negativeErrnoCode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory.CurrentWorkingDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.Stat
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.STRUCT_SIZE_PACKED_SIZE
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.packTo

public class SyscallStatLstat64FunctionHandle private constructor(
    host: EmbedderHost,
    private val followSymlinks: Boolean = false,
    function: HostFunction,
) : HostFunctionHandle(function, host) {
    public fun execute(
        memory: Memory,
        pathnamePtr: WasmPtr<Byte>,
        dstAddr: WasmPtr<StructStat>,
    ): Int {
        val path = memory.readNullTerminatedString(pathnamePtr)
        return host.fileSystem.execute(
            Stat,
            Stat(
                path = path,
                baseDirectory = CurrentWorkingDirectory,
                followSymlinks = followSymlinks,
            ),
        ).map { stat: StructStat ->
            memory.sinkWithMaxSize(dstAddr, STRUCT_SIZE_PACKED_SIZE).buffered().use {
                stat.packTo(it)
            }
        }.negativeErrnoCode()
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
