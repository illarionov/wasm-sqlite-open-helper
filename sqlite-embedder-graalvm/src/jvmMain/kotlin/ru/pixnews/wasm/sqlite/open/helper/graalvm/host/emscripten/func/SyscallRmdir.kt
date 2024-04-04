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
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno

internal class SyscallRmdir(
    language: WasmLanguage,
    module: WasmModule,
    override val host: SqliteEmbedderHost,
    functionName: String = "__syscall_rmdir",
) : BaseWasmNode(language, module, host, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Any {
        val args: Array<Any> = frame.arguments
        return syscallRmdirat(
            memory(frame),
            args.getArgAsWasmPtr(0),
        )
    }

    @TruffleBoundary
    private fun syscallRmdirat(
        memory: WasmMemory,
        pathnamePtr: WasmPtr<Byte>,
    ): Int {
        val fs = host.fileSystem
        val path = memory.readString(pathnamePtr.addr, null)
        return try {
            fs.rmdir(path)
            Errno.SUCCESS.code
        } catch (e: SysException) {
            -e.errNo.code
        }
    }
}
