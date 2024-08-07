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
import ru.pixnews.wasm.sqlite.open.helper.host.castFileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.Path
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.include.oMaskToString
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
        val fs: FileSystem<Path> = host.castFileSystem()
        val dirFd = DirFd(rawDirFd)
        val mode = FileMode(rawMode)
        val path = memory.readNullTerminatedString(pathnamePtr)
        val absolutePath = fs.resolveAbsolutePath(dirFd, path)

        return try {
            val fd = fs.open(absolutePath, flags, mode)
            logger.v { formatCallString(dirFd, path, absolutePath, flags, mode, fd) }
            fd.fd
        } catch (e: SysException) {
            logger.v {
                formatCallString(dirFd, path, absolutePath, flags, mode, null) +
                        "openAt() error ${e.errNo}"
            }
            -e.errNo.code
        }
    }

    @Suppress("MagicNumber")
    private fun formatCallString(
        dirfd: DirFd,
        path: String,
        absolutePath: Path,
        flags: UInt,
        mode: FileMode,
        fd: Fd?,
    ): String = "openAt() dirfd: " +
            "$dirfd, " +
            "path: `$path`, " +
            "full path: `$absolutePath`, " +
            "flags: 0${flags.toString(8)} (${Fcntl.oMaskToString(flags)}), " +
            "mode: $mode" +
            if (fd != null) ": $fd" else ""
}
