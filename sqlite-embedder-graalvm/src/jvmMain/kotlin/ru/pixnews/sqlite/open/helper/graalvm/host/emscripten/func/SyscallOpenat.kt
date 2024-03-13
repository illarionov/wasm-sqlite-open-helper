/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.graalvm.host.emscripten.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import java.nio.file.Path
import java.util.logging.Logger
import ru.pixnews.sqlite.open.helper.graalvm.ext.asWasmPtr
import ru.pixnews.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.sqlite.open.helper.graalvm.host.Host
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.sqlite.open.helper.host.filesystem.resolveAbsolutePath
import ru.pixnews.sqlite.open.helper.host.include.Fcntl
import ru.pixnews.sqlite.open.helper.host.include.oMaskToString
import ru.pixnews.sqlite.open.helper.host.include.sMaskToString
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class SyscallOpenat(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "__syscall_openat",
    private val logger: Logger = Logger.getLogger(SyscallOpenat::class.qualifiedName)
): BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        val mode = if (args.lastIndex == 3) {
            memory.readI32(args.asWasmPtr<UInt>(3)).toUInt()
        } else {
            0U
        }

        val fdOrErrno = openAt(
            dirfd = args[0] as Int,
            pathnamePtr = args.asWasmPtr(1),
            flags = (args[2] as Int).toUInt(),
            mode = mode,
        )
        return fdOrErrno
    }

    // XXX: copy of chikory version
    @TruffleBoundary
    private fun openAt(
        dirfd: Int,
        pathnamePtr: WasmPtr<Byte>,
        flags: UInt,
        mode: UInt
    ): Int {
        val fs = host.fileSystem
        val path = memory.readNullTerminatedString(pathnamePtr)
        val absolutePath = fs.resolveAbsolutePath(dirfd, path)

        return try {
            val fd = fs.open(absolutePath, flags, mode).fd
            logger.finest { formatCallString(dirfd, path, absolutePath, flags, mode, fd) }
            fd.fd
        } catch (e: SysException) {
            logger.finest {
                formatCallString(dirfd, path, absolutePath, flags, mode, null) +
                        "openAt() error ${e.errNo}"
            }
            -e.errNo.code
        }
    }

    private fun formatCallString(
        dirfd: Int,
        path: String,
        absolutePath: Path,
        flags: UInt,
        mode: UInt,
        fd: Fd?
    ): String = "openAt() dirfd: " +
            "$dirfd, " +
            "path: `$path`, " +
            "full path: `$absolutePath`, " +
            "flags: 0${flags.toString(8)} (${Fcntl.oMaskToString(flags)}), " +
            "mode: ${Fcntl.sMaskToString(mode)}" +
            if (fd != null) ": $fd" else ""
}
