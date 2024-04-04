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
import ru.pixnews.wasm.sqlite.open.helper.common.api.contains
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsUint
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.Sqlite3CallbackStore
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteStatement
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTrace
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_CLOSE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_PROFILE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_ROW
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_STMT
import kotlin.time.Duration.Companion.nanoseconds

internal const val SQLITE3_TRACE_CB_FUNCTION_NAME = "sqlite3_trace_cb"

internal class Sqlite3TraceAdapter(
    language: WasmLanguage,
    module: WasmModule,
    private val callbackStore: Sqlite3CallbackStore,
    override val host: SqliteEmbedderHost,
    functionName: String,
) : BaseWasmNode(language, module, host, functionName) {
    @Suppress("MagicNumber")
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

    @CompilerDirectives.TruffleBoundary
    private fun invokeTraceCallback(
        memory: WasmMemory,
        flags: SqliteTraceEventCode,
        contextPointer: WasmPtr<SqliteDb>,
        arg1: WasmPtr<Nothing>,
        arg2: Long,
    ): Int {
        val delegate: (trace: SqliteTrace) -> Unit =
            callbackStore.sqlite3TraceCallbacks[contextPointer] ?: error("Callback $contextPointer not registered")

        if (flags.contains(SQLITE_TRACE_STMT)) {
            val traceInfo = SqliteTrace.TraceStmt(
                db = contextPointer,
                statement = arg1 as WasmPtr<SqliteStatement>,
                unexpandedSql = memory.readString(arg2.toInt(), null),
            )
            delegate.invoke(traceInfo)
        }
        if (flags.contains(SQLITE_TRACE_PROFILE)) {
            val timeNs = memory.load_i64(this, arg2)
            val traceInfo = SqliteTrace.TraceProfile(
                db = contextPointer,
                statement = arg1 as WasmPtr<SqliteStatement>,
                time = timeNs.nanoseconds,
            )
            delegate.invoke(traceInfo)
        }
        if (flags.contains(SQLITE_TRACE_ROW)) {
            val traceInfo = SqliteTrace.TraceRow(
                db = contextPointer,
                statement = arg1 as WasmPtr<SqliteStatement>,
            )
            delegate.invoke(traceInfo)
        }
        if (flags.contains(SQLITE_TRACE_CLOSE)) {
            val traceInfo = SqliteTrace.TraceClose(
                db = arg1 as WasmPtr<SqliteDb>,
            )
            delegate.invoke(traceInfo)
        }

        return 0
    }
}
