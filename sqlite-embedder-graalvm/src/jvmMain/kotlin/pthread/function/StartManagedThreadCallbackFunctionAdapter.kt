/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.function

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.GraalvmManagedThreadFactory.Companion.PTHREAD_ROUTINE_CALLBACK_FUNCTION
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.WasmManagedThreadStore
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.function.StartManagedThreadCallbackFunctionAdapter.StartThreadCallbackFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle

internal class StartManagedThreadCallbackFunctionAdapter(
    language: WasmLanguage,
    module: WasmModule,
    host: EmbedderHost,
    managedThreadStore: WasmManagedThreadStore,
) : BaseWasmNode<StartThreadCallbackFunctionHandle>(
    language,
    module,
    StartThreadCallbackFunctionHandle(host, managedThreadStore),
) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, wasmInstance: WasmInstance): Int {
        val args = frame.arguments
        return handle.execute(
            args.getArgAsWasmPtr(0)
        )
    }

    internal class StartThreadCallbackFunctionHandle(
        host: EmbedderHost,
        private val managedThreadStore: WasmManagedThreadStore,
    ) : HostFunctionHandle(PTHREAD_ROUTINE_CALLBACK_FUNCTION, host) {
        @TruffleBoundary
        fun execute(arg: WasmPtr<Unit>): Int {
            logger.i { "Managed thread start_routine called with arg $arg" }

            // Start runnable?
            Thread.sleep(5000)
            return 0
        }
    }
}
