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
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.pack
import ru.pixnews.wasm.sqlite.open.helper.host.memory.write
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class SyscallFstat64(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
    functionName: String = "__syscall_fstat64",
) : BaseWasmNode(language, module, host, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, wasmInstance: WasmInstance): Int {
        val args = frame.arguments
        return syscallFstat64(
            memory(frame),
            Fd(args.getArgAsInt(0)),
            args.getArgAsWasmPtr(1),
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun syscallFstat64(
        memory: WasmMemory,
        fd: Fd,
        dst: WasmPtr<StructStat>,
    ): Int = try {
        val stat = host.fileSystem.stat(fd).also {
            logger.v { "`$fd`: OK $it" }
        }.pack()
        memory.toHostMemory().write(dst, stat)
        Errno.SUCCESS.code
    } catch (e: SysException) {
        logger.v { "`$fd`: Error ${e.errNo}" }
        -e.errNo.code
    }
}
