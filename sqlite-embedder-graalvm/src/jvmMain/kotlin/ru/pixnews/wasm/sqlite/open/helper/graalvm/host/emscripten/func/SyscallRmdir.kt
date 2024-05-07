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
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.SyscallRmdirFunctionHandle

internal class SyscallRmdir(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
) : BaseWasmNode<SyscallRmdirFunctionHandle>(language, module, SyscallRmdirFunctionHandle(host)) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Any {
        return syscallRmdirat(memory(frame), frame.arguments.getArgAsWasmPtr(0))
    }

    @TruffleBoundary
    private fun syscallRmdirat(
        memory: WasmMemory,
        pathnamePtr: WasmPtr<Byte>,
    ): Int = handle.execute(memory.toHostMemory(), pathnamePtr)
}
