/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.dsl

import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteOpenHelperDsl
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteDebug

@WasmSqliteOpenHelperDsl
public class DebugConfigBlock {
    /**
     * Controls the printing of informational SQL log messages.
     */
    public var sqlLog: Boolean = false

    /**
     * Controls the printing of SQL statements as they are executed.
     */
    public var sqlStatements: Boolean = false

    /**
     * Controls the printing of wall-clock time taken to execute SQL statements
     * as they are executed.
     */
    public var sqlTime: Boolean = false

    /**
     * True to enable database performance testing instrumentation.
     */
    public var logSlowQueries: Boolean = false

    /**
     * Value of the "db.log.slow_query_threshold" system property, which can be changed
     * by the user at any time.  If the value is zero, then all queries will
     * be considered slow.  If the value does not exist or is negative, then no queries will
     * be considered slow.
     */
    public var slowQueryThresholdProvider: () -> Int = {
        System.getProperty("db.log.slow_query_threshold", "-1")!!.toInt()
    }

    internal fun build(): SQLiteDebug = SQLiteDebug(
        sqlLog = sqlLog,
        sqlStatements = sqlStatements,
        sqlTime = sqlTime,
        logSlowQueries = logSlowQueries,
        slowQueryThresholdProvider = slowQueryThresholdProvider,
    )
}
