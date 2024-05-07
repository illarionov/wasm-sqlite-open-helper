/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.SyscallFcntl64FunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class SyscallFcntl64(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
) : BaseWasmNode<SyscallFcntl64FunctionHandle>(language, module, SyscallFcntl64FunctionHandle(host)) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, wasmInstance: WasmInstance): Int {
        val args = frame.arguments
        return syscallFcntl64(
            memory(frame),
            Fd(args.getArgAsInt(0)),
            args.getArgAsInt(1),
            args.getArgAsInt(2),
        )
    }

    @Suppress("MemberNameEqualsClassName")
    @TruffleBoundary
    private fun syscallFcntl64(
        memory: WasmMemory,
        fd: Fd,
        cmd: Int,
        thirdArg: Int,
    ): Int = handle.execute(memory.toHostMemory(), fd, cmd, thirdArg)
}
