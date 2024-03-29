/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal

/**
 * Provides debugging info about all SQLite databases running in the current process.
 *
 * {@hide}
 *
 * @property sqlLog
 *   Controls the printing of informational SQL log messages.
 * @property sqlStatements
 *   Controls the printing of SQL statements as they are executed.
 * @property sqlTime
 *   Controls the printing of wall-clock time taken to execute SQL statements
 *   as they are executed.
 * @property logSlowQueries
 *   True to enable database performance testing instrumentation.
 * @property slowQueryThresholdProvider
 *   Reads the "db.log.slow_query_threshold" system property, which can be changed
 *   by the user at any time.  If the value is zero, then all queries will
 *   be considered slow.  If the value does not exist or is negative, then no queries will
 *   be considered slow.
 *
 *   This value can be changed dynamically while the system is running.
 *   For example, "adb shell setprop db.log.slow_query_threshold 200" will
 *   log all queries that take 200ms or longer to run.
 */
internal class SQLiteDebug(
    val sqlLog: Boolean = false,
    val sqlStatements: Boolean = false,
    val sqlTime: Boolean = false,
    val logSlowQueries: Boolean = false,
    val slowQueryThresholdProvider: () -> Int,
) {
    /**
     * Determines whether a query should be logged.
     */
    internal fun shouldLogSlowQuery(elapsedTimeMillis: Long): Boolean {
        val slowQueryMillis = slowQueryThresholdProvider()
        return slowQueryMillis in 0..elapsedTimeMillis
    }
}
