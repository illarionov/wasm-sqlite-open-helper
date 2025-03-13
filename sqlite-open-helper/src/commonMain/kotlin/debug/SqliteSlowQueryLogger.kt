/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.debug

import at.released.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfigBlock.Factory.Type
import at.released.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfigBlock.Factory.Type.KEY_DEFINED
import at.released.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import at.released.wasm.sqlite.open.helper.internal.platform.getSystemProp
import at.released.weh.common.api.Logger

/**
 * Enable database performance testing instrumentation.
 */
public class SqliteSlowQueryLogger private constructor(
    public var logger: (String) -> Unit,
) : WasmSqliteDebugFeature {
    override val key: WasmSqliteDebugConfigBlock.Key<*> = SqliteStatementLogger
    public var enabled: Boolean = true

    /**
     * Value of the "db.log.slow_query_threshold" system property, which can be changed
     * by the user at any time.  If the value is zero, then all queries will
     * be considered slow.  If the value does not exist or is negative, then no queries will
     * be considered slow.
     */
    public var slowQueryThresholdProvider: () -> Int = {
        getSystemProp("db.log.slow_query_threshold", "-1").toInt()
    }

    internal fun shouldLogSlowQuery(elapsedTimeMillis: Long): Boolean {
        if (!enabled) {
            return false
        }
        val slowQueryMillis = slowQueryThresholdProvider()
        return slowQueryMillis in 0..elapsedTimeMillis
    }

    public companion object : WasmSqliteDebugConfigBlock.Key<SqliteSlowQueryLogger> {
        override fun create(commonConfig: WasmSqliteCommonConfig, type: Type): SqliteSlowQueryLogger {
            return SqliteSlowQueryLogger(createStatementLogger(commonConfig.logger)).apply {
                enabled = type == KEY_DEFINED
            }
        }

        public fun createStatementLogger(
            logger: Logger,
        ): (String) -> Unit = { message -> logger.d { message } }
    }
}
