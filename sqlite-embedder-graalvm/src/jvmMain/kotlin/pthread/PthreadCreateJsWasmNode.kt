/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.PthreadCreateJsWasmNode.PthreadCreateJsFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructPthread

internal class PthreadCreateJsWasmNode(
    language: WasmLanguage,
    module: WasmModule,
    host: EmbedderHost,
    posixThreadRef: () -> GraalvmPthreadManager,
) : BaseWasmNode<PthreadCreateJsFunctionHandle>(
    language,
    module,
    PthreadCreateJsFunctionHandle(host, posixThreadRef),
) {
    @Suppress("MagicNumber")
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Int {
        val args = frame.arguments
        return handle.execute(
            args.getArgAsWasmPtr(0),
            args.getArgAsWasmPtr(1),
            args.getArgAsInt(2),
            args.getArgAsWasmPtr(3),
        )
    }

    class PthreadCreateJsFunctionHandle(
        host: EmbedderHost,
        private val pThreadManagerRef: () -> GraalvmPthreadManager,
    ) : HostFunctionHandle(EmscriptenHostFunction.PTHREAD_CREATE_JS, host) {
        @TruffleBoundary
        fun execute(
            pthreadPtr: WasmPtr<StructPthread>,
            attr: WasmPtr<UInt>,
            startRoutine: Int,
            arg: WasmPtr<Unit>,
        ): Int {
            logger.v { "pthread_create_js(pthreadPtr=$pthreadPtr, attr=$attr, startRoutine=$startRoutine, arg=$arg)" }
            return pThreadManagerRef().spawnThread(pthreadPtr, attr, startRoutine, arg)
        }
    }
}
