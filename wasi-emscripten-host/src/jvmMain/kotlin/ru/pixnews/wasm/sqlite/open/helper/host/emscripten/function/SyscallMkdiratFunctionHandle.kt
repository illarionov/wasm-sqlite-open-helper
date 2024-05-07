/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readNullTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno

public class SyscallMkdiratFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.SYSCALL_MKDIRAT, host) {
    public fun execute(
        memory: Memory,
        rawDirFd: Int,
        pathnamePtr: WasmPtr<Byte>,
        rawMode: UInt,
    ): Int {
        val fs = host.fileSystem
        val dirFd = DirFd(rawDirFd)
        val mode = FileMode(rawMode)
        val path = memory.readNullTerminatedString(pathnamePtr)
        return try {
            fs.mkdirAt(dirFd, path, mode)
            Errno.SUCCESS.code
        } catch (e: SysException) {
            logger.v(e) { "__syscall_mkdirat($dirFd, $path, $mode) error: ${e.errNo}" }
            -e.errNo.code
        }
    }
}
