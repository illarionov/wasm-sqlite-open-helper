/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.sqlitecb.function

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function.Sqlite3LoggingFunctionHandler
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteLogCallback

internal class Sqlite3LoggingAdapter(
    language: WasmLanguage,
    module: WasmModule,
    host: EmbedderHost,
    logCallbackStore: () -> SqliteLogCallback?,
) : BaseWasmNode<Sqlite3LoggingFunctionHandler>(
    language,
    module,
    Sqlite3LoggingFunctionHandler(host, logCallbackStore),
) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, wasmInstance: WasmInstance) {
        val args = frame.arguments
        invokeLoggingCallback(
            memory(frame),
            // unused context pointer args.getArgAsWasmPtr(0),
            args.getArgAsInt(1),
            args.getArgAsWasmPtr(2),
        )
    }

    @TruffleBoundary
    private fun invokeLoggingCallback(
        memory: WasmMemory,
        errCode: Int,
        messagePointer: WasmPtr<Byte>,
    ) = handle.execute(memory.toHostMemory(), errCode, messagePointer)
}
