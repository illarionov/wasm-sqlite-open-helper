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
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.EmscriptenInitMainThreadJs.InitMainThreadJsHandle
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.pthread.Pthread
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction

internal class EmscriptenInitMainThreadJs(
    language: WasmLanguage,
    module: WasmModule,
    host: EmbedderHost,
    posixThreadRef: () -> Pthread,
) : BaseWasmNode<InitMainThreadJsHandle>(language, module, InitMainThreadJsHandle(host, posixThreadRef)) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance) {
        val args = frame.arguments
        handle.execute(args.getArgAsWasmPtr(0))
    }

    class InitMainThreadJsHandle(
        host: EmbedderHost,
        private val posixThreadRef: () -> Pthread,
    ) : HostFunctionHandle(EmscriptenHostFunction.EMSCRIPTEN_INIT_MAIN_THREAD_JS, host) {
        @TruffleBoundary
        fun execute(ptr: WasmPtr<Unit>) {
            val pthread = posixThreadRef()
            pthread.emscriptenThreadInit(
                ptr,
                pthread.isMainThread(),
                true,
                true,
                Pthread.DEFAULT_THREAD_STACK_SIZE,
                false,
            )
            pthread.emscriptenThreadLocalStorageInit()
        }
    }
}
