/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.debug

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfigBlock.Factory.Type
import ru.pixnews.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfigBlock.Factory.Type.KEY_DEFINED
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig

/**
 * Enables logging of SQL statement at the moment before it starts executing
 *
 * Uses SQLITE_TRACE_STMT, SQLITE_TRACE_ROW, SQLITE_TRACE_CLOSE callbacks of [SQLite Trace](https://sqlite.org/c3ref/c_trace.html)
 * under the hood.
 */
public class SqliteStatementLogger private constructor(
    public var logger: (TraceEvent) -> Unit,
) : WasmSqliteDebugFeature {
    @InternalWasmSqliteHelperApi
    override val key: WasmSqliteDebugConfigBlock.Key<*> = Companion
    public var enabled: Boolean = true

    public sealed class TraceEvent {
        public class Statement(
            public val db: Any,
            public val sql: String,
        ) : TraceEvent() {
            override fun toString(): String = """$db: "$sql""""
        }

        public class Row(
            public val db: Any,
            public val statementId: Any,
        ) : TraceEvent() {
            override fun toString(): String = """$db / $statementId: Received row"""
        }

        public class Close(
            public val db: Any,
        ) : TraceEvent() {
            override fun toString(): String = """"$db closed""""
        }
    }

    public companion object : WasmSqliteDebugConfigBlock.Key<SqliteStatementLogger> {
        override fun create(commonConfig: WasmSqliteCommonConfig, type: Type): SqliteStatementLogger {
            val logger = commonConfig.logger.withTag("SQL TRACE")
            return SqliteStatementLogger(createStatementLogger(logger)).apply {
                enabled = type == KEY_DEFINED
            }
        }

        public fun createStatementLogger(
            logger: Logger,
        ): (TraceEvent) -> Unit = { event -> logger.d { event.toString() } }
    }
}
