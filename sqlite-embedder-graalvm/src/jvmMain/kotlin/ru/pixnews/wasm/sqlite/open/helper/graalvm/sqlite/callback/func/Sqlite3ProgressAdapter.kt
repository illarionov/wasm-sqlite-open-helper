/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func

import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.Sqlite3CallbackStore
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback

internal const val SQLITE3_PROGRESS_CB_FUNCTION_NAME = "sqlite3_progress_cb"

internal class Sqlite3ProgressAdapter(
    language: WasmLanguage,
    module: WasmModule,
    private val callbackStore: Sqlite3CallbackStore,
    host: SqliteEmbedderHost,
    functionName: String,
) : BaseWasmNode(language, module, host, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, wasmInstance: WasmInstance): Int {
        val args = frame.arguments
        return invokeProgressCallback(
            args.getArgAsWasmPtr(0),
        )
    }

    @CompilerDirectives.TruffleBoundary
    private fun invokeProgressCallback(
        contextPointer: WasmPtr<SqliteDb>,
    ): Int {
        logger.v { "invokeProgressCallback() db: $contextPointer" }
        val delegate: SqliteProgressCallback = callbackStore.sqlite3ProgressCallbacks[contextPointer]
            ?: error("Callback $contextPointer not registered")

        return delegate.invoke(contextPointer)
    }
}
