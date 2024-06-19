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
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.SyscallStatLstat64FunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.StructStat

internal fun syscallLstat64(
    language: WasmLanguage,
    module: WasmModule,
    host: EmbedderHost,
): BaseWasmNode<SyscallStatLstat64FunctionHandle> = SyscallStat64(
    language = language,
    module = module,
    handle = SyscallStatLstat64FunctionHandle.syscallLstat64(host),
)

internal fun syscallStat64(
    language: WasmLanguage,
    module: WasmModule,
    host: EmbedderHost,
): BaseWasmNode<SyscallStatLstat64FunctionHandle> = SyscallStat64(
    language = language,
    module = module,
    handle = SyscallStatLstat64FunctionHandle.syscallStat64(host),
)

private class SyscallStat64(
    language: WasmLanguage,
    module: WasmModule,
    handle: SyscallStatLstat64FunctionHandle,
) : BaseWasmNode<SyscallStatLstat64FunctionHandle>(language, module, handle) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Int {
        val args = frame.arguments
        return stat64(
            memory(frame),
            args.getArgAsWasmPtr(0),
            args.getArgAsWasmPtr(1),
        )
    }

    @TruffleBoundary
    private fun stat64(
        memory: WasmMemory,
        pathnamePtr: WasmPtr<Byte>,
        dst: WasmPtr<StructStat>,
    ): Int = handle.execute(memory.toHostMemory(), pathnamePtr, dst)
}
