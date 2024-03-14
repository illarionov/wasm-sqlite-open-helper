/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.func

import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.sqlite.open.helper.common.api.contains
import ru.pixnews.sqlite.open.helper.graalvm.ext.asWasmPtr
import ru.pixnews.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.Sqlite3CallbackStore
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteStatement
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteTrace
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_CLOSE
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_PROFILE
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_ROW
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_STMT
import java.util.logging.Logger
import kotlin.time.Duration.Companion.milliseconds

internal const val SQLITE3_TRACE_CB_FUNCTION_NAME = "sqlite3_trace_cb"

internal class Sqlite3TraceAdapter(
    language: WasmLanguage,
    instance: WasmInstance,
    private val callbackStore: Sqlite3CallbackStore,
    functionName: String,
    private val logger: Logger = Logger.getLogger(Sqlite3TraceAdapter::class.qualifiedName),
) : BaseWasmNode(language, instance, functionName) {
    @Suppress("MagicNumber")
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return invokeTraceCallback(
            SqliteTraceEventCode(args[0] as UInt),
            args.asWasmPtr(1),
            args.asWasmPtr(2),
            (args[3] as Int).toLong(),
        )
    }

    @CompilerDirectives.TruffleBoundary
    private fun invokeTraceCallback(
        flags: SqliteTraceEventCode,
        contextPointer: WasmPtr<SqliteDb>,
        arg1: WasmPtr<Nothing>,
        arg2: Long,
    ): Int {
        logger.finest { "invokeTraceCallback() flags: $flags db: $contextPointer arg1: $arg1 arg3: $arg2" }
        val delegate: (trace: SqliteTrace) -> Unit =
            callbackStore.sqlite3TraceCallbacks[contextPointer] ?: error("Callback $contextPointer not registered")

        if (flags.contains(SQLITE_TRACE_STMT)) {
            val traceInfo = SqliteTrace.TraceStmt(
                db = contextPointer,
                statement = arg1 as WasmPtr<SqliteStatement>,
                unexpandedSql = memory.readNullTerminatedString(WasmPtr(arg2.toInt())),
            )
            delegate.invoke(traceInfo)
        }
        if (flags.contains(SQLITE_TRACE_PROFILE)) {
            val traceInfo = SqliteTrace.TraceProfile(
                db = contextPointer,
                statement = arg1 as WasmPtr<SqliteStatement>,
                time = arg2.milliseconds,
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
