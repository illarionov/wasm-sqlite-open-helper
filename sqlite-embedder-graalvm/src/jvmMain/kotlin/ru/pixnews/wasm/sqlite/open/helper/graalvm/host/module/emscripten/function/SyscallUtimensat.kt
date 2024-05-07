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
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsUint
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.SyscallUtimensatFunctionHandle

internal class SyscallUtimensat(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
) : BaseWasmNode<SyscallUtimensatFunctionHandle>(language, module, SyscallUtimensatFunctionHandle(host)) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Any {
        val args: Array<Any> = frame.arguments
        return syscallUtimensat(
            memory(frame),
            args.getArgAsInt(0),
            args.getArgAsWasmPtr(1),
            args.getArgAsWasmPtr(2),
            args.getArgAsUint(3),
        )
    }

    @Suppress("MemberNameEqualsClassName")
    @TruffleBoundary
    private fun syscallUtimensat(
        memory: WasmMemory,
        rawDirFd: Int,
        pathnamePtr: WasmPtr<Byte>,
        times: WasmPtr<Byte>,
        flags: UInt,
    ): Int = handle.execute(memory.toHostMemory(), rawDirFd, pathnamePtr, times, flags)
}
