/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.graalvm.host.module.sqlitecb.function

import at.released.wasm.sqlite.open.helper.WasmPtr
import at.released.wasm.sqlite.open.helper.embedder.sqlitecb.function.Sqlite3ProgressFunctionHandle
import at.released.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import at.released.wasm.sqlite.open.helper.graalvm.host.module.BaseWasmNode
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback
import at.released.weh.host.EmbedderHost
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule

internal class Sqlite3ProgressAdapter(
    language: WasmLanguage,
    module: WasmModule,
    host: EmbedderHost,
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
