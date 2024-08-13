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
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.Open
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.OpenError
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

public class SyscallOpenatFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.SYSCALL_OPENAT, host) {
    public fun execute(
        memory: ReadOnlyMemory,
        rawDirFd: Int,
        pathnamePtr: WasmPtr<Byte>,
        flags: UInt,
        rawMode: UInt,
    ): Int {
        val fs = host.fileSystem
        val baseDirectory = BaseDirectory.fromRawDirFd(rawDirFd)
        val mode = FileMode(rawMode)
        val path = memory.readNullTerminatedString(pathnamePtr)

        val fsOperation = Open(
            path = path,
            baseDirectory = baseDirectory,
            flags = flags,
            mode = mode,
        )
        return fs.execute(Open, fsOperation)
            .onLeft {
                logger.v {
                    "$fsOperation error ${it.errno}, ${it.message}"
                }
            }.onRight {
                logger.v { "$fsOperation; fd: $it" }
            }.fold(
                ifLeft = { error: OpenError ->
                    -error.errno.code
                },
                ifRight = { fd: Fd ->
                    fd.fd
                },
            )
    }
}
