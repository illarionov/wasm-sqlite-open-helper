/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.graalvm.host.module.sqlitecb.function

import at.released.wasm.sqlite.open.helper.WasmPtr
import at.released.wasm.sqlite.open.helper.embedder.sqlitecb.function.Sqlite3TraceFunctionHandle
import at.released.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import at.released.wasm.sqlite.open.helper.graalvm.ext.getArgAsUint
import at.released.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import at.released.wasm.sqlite.open.helper.graalvm.host.module.BaseWasmNode
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceCallback
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode
import at.released.weh.host.EmbedderHost
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory

internal class Sqlite3TraceAdapter(
    language: WasmLanguage,
    module: WasmModule,
    host: EmbedderHost,
    traceCallbackStore: (WasmPtr<SqliteDb>) -> SqliteTraceCallback?,
) : BaseWasmNode<Sqlite3TraceFunctionHandle>(language, module, Sqlite3TraceFunctionHandle(host, traceCallbackStore)) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, wasmInstance: WasmInstance): Int {
        val args = frame.arguments
        return invokeTraceCallback(
            memory(frame),
            SqliteTraceEventCode(args.getArgAsUint(0)),
            args.getArgAsWasmPtr(1),
            args.getArgAsWasmPtr(2),
            (args.getArgAsInt(3)).toLong(),
        )
    }

    @TruffleBoundary
    private fun invokeTraceCallback(
        memory: WasmMemory,
        flags: SqliteTraceEventCode,
        contextPointer: WasmPtr<SqliteDb>,
        arg1: WasmPtr<Nothing>,
        arg2: Long,
    ): Int = handle.execute(memory.toHostMemory(), flags, contextPointer, arg1, arg2)
}
