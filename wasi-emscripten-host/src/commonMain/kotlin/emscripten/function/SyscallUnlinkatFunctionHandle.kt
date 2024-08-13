/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.ReadOnlyMemory
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.readNullTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.ext.negativeErrnoCode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.UnlinkDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.UnlinkFile
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl.AT_REMOVEDIR

public class SyscallUnlinkatFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.SYSCALL_UNLINKAT, host) {
    public fun execute(
        memory: ReadOnlyMemory,
        rawDirfd: Int,
        pathnamePtr: WasmPtr<Byte>,
        flags: UInt,
    ): Int {
        val path = memory.readNullTerminatedString(pathnamePtr)
        val baseDirectory = BaseDirectory.fromRawDirFd(rawDirfd)
        return if (flags and AT_REMOVEDIR == AT_REMOVEDIR) {
            host.fileSystem.execute(
                operation = UnlinkDirectory,
                input = UnlinkDirectory(
                    path = path,
                    baseDirectory = baseDirectory,
                ),
            )
        } else {
            host.fileSystem.execute(
                operation = UnlinkFile,
                input = UnlinkFile(
                    path = path,
                    baseDirectory = baseDirectory,
                ),
            )
        }.negativeErrnoCode()
    }
}
