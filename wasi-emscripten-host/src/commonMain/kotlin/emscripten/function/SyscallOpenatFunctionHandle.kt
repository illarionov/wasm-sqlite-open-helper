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
import ru.pixnews.wasm.sqlite.open.helper.host.ext.fromRawDirFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.open.Open
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.open.OpenError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.open.OpenFileFlags

public class SyscallOpenatFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.SYSCALL_OPENAT, host) {
    public fun execute(
        memory: ReadOnlyMemory,
        rawDirFd: Int,
        pathnamePtr: WasmPtr<Byte>,
        rawFlags: UInt,
        rawMode: UInt,
    ): Int {
        val fs = host.fileSystem
        val baseDirectory = BaseDirectory.fromRawDirFd(rawDirFd)
        val mode = FileMode(rawMode)
        val flags = OpenFileFlags(rawFlags)
        val path = memory.readNullTerminatedString(pathnamePtr)

        val fsOperation = Open(
            path = path,
            baseDirectory = baseDirectory,
            flags = flags,
            mode = mode,
        )
        return fs.execute(Open, fsOperation)
            .fold(
                ifLeft = { error: OpenError ->
                    -error.errno.code
                },
                ifRight = { fd: Fd ->
                    fd.fd
                },
            )
    }
}
