/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmArguments
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsUint
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.resolveAbsolutePath
import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.include.oMaskToString
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import java.nio.file.Path

internal class SyscallOpenat(
    language: WasmLanguage,
    module: WasmModule,
    private val host: SqliteEmbedderHost,
    functionName: String = "__syscall_openat",
) : BaseWasmNode(language, module, functionName) {
    private val logger: Logger = host.rootLogger.withTag(SyscallOpenat::class.qualifiedName!!)

    @Suppress("MagicNumber")
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Any {
        val args = frame.arguments
        val memory = memory(frame)
        val mode = if (WasmArguments.getArgumentCount(args) >= 4) {
            memory.load_i32(this, args.getArgAsInt(3).toLong()).toUInt()
        } else {
            0U
        }

        val fdOrErrno = openAt(
            memory,
            rawDirFd = args.getArgAsInt(0),
            pathnamePtr = args.getArgAsWasmPtr(1),
            flags = args.getArgAsUint(2),
            rawMode = mode,
        )
        return fdOrErrno
    }

    @TruffleBoundary
    private fun openAt(
        memory: WasmMemory,
        rawDirFd: Int,
        pathnamePtr: WasmPtr<Byte>,
        flags: UInt,
        rawMode: UInt,
    ): Int {
        val fs = host.fileSystem
        val dirFd = DirFd(rawDirFd)
        val mode = FileMode(rawMode)
        val path = memory.readString(pathnamePtr.addr, null)
        val absolutePath = fs.resolveAbsolutePath(dirFd, path)

        return try {
            val fd = fs.open(absolutePath, flags, mode).fd
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
