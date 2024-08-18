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
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.SyscallFchown32FunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd

internal class SyscallFchown32(
    language: WasmLanguage,
    module: WasmModule,
    host: EmbedderHost,
) : BaseWasmNode<SyscallFchown32FunctionHandle>(language, module, SyscallFchown32FunctionHandle(host)) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Int {
        val args = frame.arguments
        return syscallFchown32(
            args.getArgAsInt(0),
            args.getArgAsInt(1),
            args.getArgAsInt(2),
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun syscallFchown32(
        fd: Int,
        owner: Int,
        group: Int,
    ): Int = handle.execute(Fd(fd), owner, group)
}
