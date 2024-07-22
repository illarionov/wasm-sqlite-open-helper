/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.sinkWithMaxSize
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.ext.encodeToNullTerminatedBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno

public class SyscallGetcwdFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.SYSCALL_GETCWD, host) {
    public fun execute(
        memory: Memory,
        dst: WasmPtr<Byte>,
        size: Int,
    ): Int {
        logger.v { "getCwd(dst: $dst size: $size)" }
        if (size == 0) {
            return -Errno.INVAL.code
        }

        val path = host.fileSystem.getCwdPath().pathString
        val pathBuffer = path.encodeToNullTerminatedBuffer()

        if (size < pathBuffer.size) {
            return -Errno.RANGE.code
        }

        val pathSize = pathBuffer.size.toInt()
        memory.sinkWithMaxSize(dst, pathSize).use {
            it.write(pathBuffer, pathSize.toLong())
        }
        return pathSize
    }
}
