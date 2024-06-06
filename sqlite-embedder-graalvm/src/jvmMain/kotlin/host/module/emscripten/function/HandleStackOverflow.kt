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
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.stack.EmscriptenStack
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.HandleStackOverflowFunctionHandle

internal class HandleStackOverflow(
    language: WasmLanguage,
    module: WasmModule,
    host: EmbedderHost,
    private val stackBindingsRef: () -> EmscriptenStack,
) : BaseWasmNode<HandleStackOverflowFunctionHandle>(language, module, HandleStackOverflowFunctionHandle(host)) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Nothing {
        handleStackOverflow(frame.arguments.getArgAsWasmPtr(0))
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun handleStackOverflow(
        requestedSp: WasmPtr<Byte>,
    ): Nothing = handle.execute(stackBindingsRef(), requestedSp)
}
