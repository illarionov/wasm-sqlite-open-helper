/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.contains
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readNullTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteStatement
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTrace
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_CLOSE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_PROFILE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_ROW
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_STMT
import kotlin.time.Duration.Companion.nanoseconds

public class Sqlite3TraceFunctionHandle(
    host: EmbedderHost,
    private val traceCallbackStore: (WasmPtr<SqliteDb>) -> SqliteTraceCallback?,
) : HostFunctionHandle(SqliteCallbacksModuleFunction.SQLITE3_LOGGING_CALLBACK, host) {
    public fun execute(
        memory: Memory,
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
                unexpandedSql = memory.readNullTerminatedString(WasmPtr(arg2.toInt())),
            )
            delegate.invoke(traceInfo)
        }
        if (flags.contains(SQLITE_TRACE_PROFILE)) {
            val timeNs = memory.readI64(WasmPtr<Long>(arg2.toInt()))
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
