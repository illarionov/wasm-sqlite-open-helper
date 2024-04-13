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
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.Sqlite3CallbackStore

internal const val SQLITE3_LOGGING_CB_FUNCTION_NAME = "sqlite3_logging_cb"

internal class Sqlite3LoggingAdapter(
    language: WasmLanguage,
    module: WasmModule,
    private val callbackStore: Sqlite3CallbackStore,
    host: SqliteEmbedderHost,
    functionName: String,
) : BaseWasmNode(language, module, host, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, wasmInstance: WasmInstance) {
        val args = frame.arguments
        invokeLoggingCallback(
            memory(frame),
            // unused context pointer args.getArgAsWasmPtr(0),
            args.getArgAsInt(1),
            args.getArgAsWasmPtr(2),
        )
    }

    @CompilerDirectives.TruffleBoundary
    private fun invokeLoggingCallback(
        memory: WasmMemory,
        errCode: Int,
        messagePointer: WasmPtr<Byte>,
    ) {
        val delegate = callbackStore.sqlite3LogCallback ?: return
        val message = memory.readString(messagePointer.addr, null)
        delegate.invoke(errCode, message)
    }
}
