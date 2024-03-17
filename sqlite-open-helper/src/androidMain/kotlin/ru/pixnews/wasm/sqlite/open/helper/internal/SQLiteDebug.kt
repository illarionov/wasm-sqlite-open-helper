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
// TODO: Set?
public class SQLiteDebug(
    public val sqlLog: Boolean = false,
    public val sqlStatements: Boolean = false,
    public val sqlTime: Boolean = false,
    public val logSlowQueries: Boolean = false,
    public val slowQueryThresholdProvider: () -> Int = {
        System.getProperty("db.log.slow_query_threshold", "-1")!!.toInt()
    },
) {
    /**
     * Determines whether a query should be logged.
     */
    internal fun shouldLogSlowQuery(elapsedTimeMillis: Long): Boolean {
        val slowQueryMillis = slowQueryThresholdProvider()
        return slowQueryMillis in 0..elapsedTimeMillis
    }
}

/**
 * contains statistics about a database
 *
 * @property dbName name of the database
 * @property lookaside documented here http://www.sqlite.org/c3ref/c_dbstatus_lookaside_used.html
 */
internal class DbStats(
    val dbName: String,
    pageCount: Long,
    pageSize: Long,
    val lookaside: Int,
    hits: Int,
    misses: Int,
    cachesize: Int,
) {
    /** the page size for the database  */
    val pageSize: Long = pageSize / 1024

    /** the database size  */
    val dbSize: Long = pageCount * pageSize / 1024

    /** statement cache stats: hits/misses/cachesize  */
    val cache: String = "$hits/$misses/$cachesize"
}
