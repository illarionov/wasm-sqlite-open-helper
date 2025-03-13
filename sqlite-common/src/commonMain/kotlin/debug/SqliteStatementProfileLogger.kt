/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.debug

import at.released.weh.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfigBlock.Factory.Type
import ru.pixnews.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfigBlock.Factory.Type.KEY_DEFINED
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import kotlin.time.Duration

/**
 * Enables logging of the SQL statement with wall-clock time it took to execute.
 *
 * Uses [SQLITE_TRACE_PROFILE](https://sqlite.org/c3ref/c_trace.html) callback under the hood.
 */
public class SqliteStatementProfileLogger private constructor(
    public var logger: (db: Any, sql: String, time: Duration) -> Unit,
) : WasmSqliteDebugFeature {
    @InternalWasmSqliteHelperApi
    override val key: WasmSqliteDebugConfigBlock.Key<*> = Companion
    public var enabled: Boolean = true

    public companion object : WasmSqliteDebugConfigBlock.Key<SqliteStatementProfileLogger> {
        override fun create(commonConfig: WasmSqliteCommonConfig, type: Type): SqliteStatementProfileLogger {
            val logger = commonConfig.logger.withTag("SQL PROFILE")
            return SqliteStatementProfileLogger(createStatementLogger(logger)).apply {
                enabled = type == KEY_DEFINED
            }
        }

        public fun createStatementLogger(
            logger: Logger,
        ): (Any, String, Duration) -> Unit = { db, sql, time -> logger.d { """$db: "$sql" took $time""" } }
    }
}
