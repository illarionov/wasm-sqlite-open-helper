/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import kotlinx.io.Buffer
import kotlinx.io.writeString
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.readNullTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.sinkWithMaxSize
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.ext.fromRawDirFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readlink.ReadLink

public class SyscallReadlinkatFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.SYSCALL_READLINKAT, host) {
    public fun execute(
        memory: Memory,
        rawDirFd: Int,
        pathnamePtr: WasmPtr<Byte>,
        buf: WasmPtr<Byte>,
        bufSize: Int,
    ): Int {
        val path = memory.readNullTerminatedString(pathnamePtr)

        if (bufSize < 0) {
            return -Errno.INVAL.code
        }

        return host.fileSystem.execute(
            ReadLink,
            ReadLink(
                path = path,
                baseDirectory = BaseDirectory.fromRawDirFd(rawDirFd),
            ),
        ).fold(
            ifLeft = { -it.errno.code },
        ) { linkPath: String ->
            val linkpathBuffer = Buffer().also { it.writeString(linkPath.toString()) }
            val len = linkpathBuffer.size.toInt().coerceAtMost(bufSize)

            memory.sinkWithMaxSize(buf, len).use {
                it.write(linkpathBuffer, len.toLong())
            }
            len
        }
    }
}
