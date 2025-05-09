/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.embedder.sqlitecb.function

import at.released.wasm.sqlite.open.helper.WasmPtr
import at.released.wasm.sqlite.open.helper.contains
import at.released.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteStatement
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTrace
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceCallback
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_CLOSE
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_PROFILE
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_ROW
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_STMT
import at.released.weh.host.EmbedderHost
import at.released.weh.wasm.core.memory.ReadOnlyMemory
import at.released.weh.wasm.core.memory.readNullTerminatedString
import kotlin.time.Duration.Companion.nanoseconds

public class Sqlite3TraceFunctionHandle(
    host: EmbedderHost,
    private val traceCallbackStore: (WasmPtr<SqliteDb>) -> SqliteTraceCallback?,
) : SqliteHostFunctionHandle(SqliteCallbacksModuleFunction.SQLITE3_LOGGING_CALLBACK, host) {
    public fun execute(
        memory: ReadOnlyMemory,
        flags: SqliteTraceEventCode,
        contextPointer: WasmPtr<SqliteDb>,
        arg1: WasmPtr<Nothing>,
        arg2: Long,
    ): Int {
        val delegate: (trace: SqliteTrace) -> Unit =
            traceCallbackStore(contextPointer) ?: error("Callback $contextPointer not registered")

        if (flags.contains(SQLITE_TRACE_STMT)) {
            val traceInfo = SqliteTrace.TraceStmt(
                db = contextPointer,
                statement = arg1 as WasmPtr<SqliteStatement>,
                unexpandedSql = memory.readNullTerminatedString(arg2.toInt()),
            )
            delegate.invoke(traceInfo)
        }
        if (flags.contains(SQLITE_TRACE_PROFILE)) {
            val timeNs = memory.readI64(arg2.toInt())
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
