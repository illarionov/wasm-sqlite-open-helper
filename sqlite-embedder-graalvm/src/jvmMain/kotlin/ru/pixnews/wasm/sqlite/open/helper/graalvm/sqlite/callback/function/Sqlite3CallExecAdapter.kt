/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.function

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore.SqliteExecCallbackId
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function.SqliteExecCallbackFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteExecCallback

internal class Sqlite3CallExecAdapter(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
    execCallbackStore: (SqliteExecCallbackId) -> SqliteExecCallback?,
) : BaseWasmNode<SqliteExecCallbackFunctionHandle>(
    language,
    module,
    SqliteExecCallbackFunctionHandle(host, execCallbackStore),
) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Any {
        val args = frame.arguments
        return callDelegate(
            memory(frame),
            args.getArgAsInt(0),
            args.getArgAsInt(1),
            args.getArgAsWasmPtr(2),
            args.getArgAsWasmPtr(3),
        )
    }

    @TruffleBoundary
    private fun callDelegate(
        memory: WasmMemory,
        arg1: Int,
        columns: Int,
        pResults: WasmPtr<WasmPtr<Byte>>,
        pColumnNames: WasmPtr<WasmPtr<Byte>>,
    ): Int {
        return handle.execute(memory.toHostMemory(), arg1, columns, pResults, pColumnNames)
    }
}
