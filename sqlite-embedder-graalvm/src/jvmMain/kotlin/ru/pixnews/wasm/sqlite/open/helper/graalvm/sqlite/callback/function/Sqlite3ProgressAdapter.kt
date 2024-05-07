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
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function.Sqlite3ProgressFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback

internal class Sqlite3ProgressAdapter(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
    progressCallbackStore: (WasmPtr<SqliteDb>) -> SqliteProgressCallback?,
) : BaseWasmNode<Sqlite3ProgressFunctionHandle>(
    language,
    module,
    Sqlite3ProgressFunctionHandle(host, progressCallbackStore),
) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, wasmInstance: WasmInstance): Int {
        return invokeProgressCallback(frame.arguments.getArgAsWasmPtr(0))
    }

    @TruffleBoundary
    private fun invokeProgressCallback(contextPointer: WasmPtr<SqliteDb>): Int = handle.execute(contextPointer)
}
