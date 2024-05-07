/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.FcntlHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

public class SyscallFcntl64FunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.SYSCALL_FCNTL64, host) {
    private val fcntlHandler = FcntlHandler(host.fileSystem, host.rootLogger)

    public fun execute(
        memory: Memory,
        fd: Fd,
        cmd: Int,
        thirdArg: Int,
    ): Int {
        return try {
            fcntlHandler.invoke(memory, fd, cmd.toUInt(), thirdArg)
        } catch (e: SysException) {
            logger.v(e) { "__syscall_fcntl64() failed: ${e.message}" }
            e.errNo.code
        }
    }
}
