/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api

import ru.pixnews.wasm.sqlite.open.helper.common.api.SqliteUintBitMask
import kotlin.jvm.JvmInline

/**
 * SQL Trace Event Codes
 *
 * https://www.sqlite.org/c3ref/c_trace.html
 */
@JvmInline
public value class SqliteTraceEventCode(
    override val mask: UInt,
) : SqliteUintBitMask<SqliteTraceEventCode> {
    override val newInstance: (UInt) -> SqliteTraceEventCode get() = ::SqliteTraceEventCode

    override fun toString(): String = "0x${mask.toString(16)}"

    public companion object {
        /**
         * An SQLITE_TRACE_STMT callback is invoked when a prepared statement first begins running and possibly
         * at other times during the execution of the prepared statement, such as at the start of each trigger
         * subprogram
         */
        public val SQLITE_TRACE_STMT: SqliteTraceEventCode = SqliteTraceEventCode(0x01U)

        /**
         * An SQLITE_TRACE_PROFILE callback provides approximately the same information as is provided by the
         * sqlite3_profile() callback.
         * The P argument is a pointer to the prepared statement and the X argument points to
         * a 64-bit integer which is approximately the number of nanoseconds that the prepared statement
         * took to run. The SQLITE_TRACE_PROFILE callback is invoked when the statement finishes.
         *
         */
        public val SQLITE_TRACE_PROFILE: SqliteTraceEventCode = SqliteTraceEventCode(0x02U)

        /**
         * An SQLITE_TRACE_ROW callback is invoked whenever a prepared statement generates a single row of result.
         * The P argument is a pointer to the prepared statement and the X argument is unused.
         */
        public val SQLITE_TRACE_ROW: SqliteTraceEventCode = SqliteTraceEventCode(0x04U)

        /**
         * An SQLITE_TRACE_CLOSE callback is invoked when a database connection closes.
         * The P argument is a pointer to the database connection object and the X argument is unused.
         */
        public val SQLITE_TRACE_CLOSE: SqliteTraceEventCode = SqliteTraceEventCode(0x08U)
    }
}
