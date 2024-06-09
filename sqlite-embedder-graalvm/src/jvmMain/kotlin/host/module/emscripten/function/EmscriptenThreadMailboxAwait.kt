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
import ru.pixnews.wasm.sqlite.open.helper.graalvm.bindings.GraalvmPthread
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.EmscriptenThreadMailboxAwait.EmscriptenThreadMailboxAwaitHandle
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction

internal class EmscriptenThreadMailboxAwait(
    language: WasmLanguage,
    module: WasmModule,
    host: EmbedderHost,
    @Suppress("UnusedPrivateProperty")
    private val posixThreadRef: () -> GraalvmPthread,
) : BaseWasmNode<EmscriptenThreadMailboxAwaitHandle>(language, module, EmscriptenThreadMailboxAwaitHandle(host)) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance) {
        handle.execute(frame.arguments.getArgAsInt(0))
    }

    class EmscriptenThreadMailboxAwaitHandle(
        host: EmbedderHost,
    ) : HostFunctionHandle(EmscriptenHostFunction.EMSCRIPTEN_INIT_MAIN_THREAD_JS, host) {
        @TruffleBoundary
        fun execute(threadPtr: Int) {
            logger.v { "_emscripten_thread_mailbox_await($threadPtr): skip, not implemented" }
        }
    }
}
