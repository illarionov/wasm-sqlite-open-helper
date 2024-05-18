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
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileAccessibilityCheck
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno

public class SyscallFaccessatFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.SYSCALL_FACCESSAT, host) {
    public fun execute(
        memory: Memory,
        rawDirFd: Int,
        pathnamePtr: WasmPtr<Byte>,
        amode: UInt,
        flags: UInt,
    ): Int {
        val fs = host.fileSystem
        val dirFd = DirFd(rawDirFd)
        val path = memory.readNullTerminatedString(pathnamePtr)
        val check = FileAccessibilityCheck(amode)
        return try {
            fs.faccessat(dirFd, path, check, flags)
            Errno.SUCCESS.code
        } catch (e: SysException) {
            logger.v { "__syscall_faccessat($rawDirFd, $path, $check, $flags) failed: ${e.errNo}" }
            -e.errNo.code
        }
    }
}
