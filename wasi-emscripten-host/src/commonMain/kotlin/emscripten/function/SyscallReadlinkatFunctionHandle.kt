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
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.readNullTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.host.castFileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.Path
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno

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
        val fs: FileSystem<Path> = host.castFileSystem()
        val dirFd = DirFd(rawDirFd)
        val path = memory.readNullTerminatedString(pathnamePtr)

        if (bufSize < 0) {
            return -Errno.INVAL.code
        }

        return try {
            val linkPath = fs.readLinkAt(dirFd, path)
            val linkpathByteArray = linkPath.encodeToByteArray()
            val len = linkpathByteArray.size.coerceAtMost(bufSize)
            memory.write(buf, linkpathByteArray, 0, len)
            len
        } catch (e: SysException) {
            logger.v {
                "readlinkat(rawdirfd: $rawDirFd path: $path, buf: $buf, bufsiz: $bufSize) error: ${e.errNo}"
            }
            -e.errNo.code
        }
    }
}
