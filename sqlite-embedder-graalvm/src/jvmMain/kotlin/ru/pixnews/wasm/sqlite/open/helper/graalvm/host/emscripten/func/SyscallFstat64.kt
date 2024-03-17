/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.asWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.pack
import ru.pixnews.wasm.sqlite.open.helper.host.memory.write
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class SyscallFstat64(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: SqliteEmbedderHost,
    functionName: String = "__syscall_fstat64",
    private val logger: Logger = Logger.withTag(SyscallFstat64::class.qualifiedName!!),
) : BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return syscallFstat64(
            Fd(args[0] as Int),
            args.asWasmPtr(1),
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun syscallFstat64(
        fd: Fd,
        dst: WasmPtr<StructStat>,
    ): Int = try {
        val stat = host.fileSystem.stat(fd).also {
            logger.v { "fStat64($fd): OK $it" }
        }.pack()
        memory.write(dst, stat)
        Errno.SUCCESS.code
    } catch (e: SysException) {
        logger.v { "fStat64($fd): Error ${e.errNo}" }
        -e.errNo.code
    }
}
