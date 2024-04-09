/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.connection

import androidx.collection.LruCache
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.SqlOpenHelperNativeBindings
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3ConnectionPtr
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3StatementPtr
import ru.pixnews.wasm.sqlite.open.helper.internal.platform.synchronized

internal class PreparedStatementCache<CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr>(
    private val connectionPtr: CP,
    private val bindings: SqlOpenHelperNativeBindings<CP, SP>,
    size: Int,
) : LruCache<String, PreparedStatement<SP>>(size) {
    private val lock = Any()

    // The database sequence number.  This changes every time the database schema changes.
    var databaseSeqNum: Long = 0
        set(value) = synchronized(lock) { field = value }

    // The database sequence number from the last getStatement() or createStatement()
    // call. The proper use of this variable depends on the caller being single threaded
    var lastSeqNum: Long = 0
        private set

    fun getStatement(sql: String) = synchronized(lock) {
        lastSeqNum = databaseSeqNum
        get(sql)
    }

    fun createStatement(sql: String): SP = synchronized(lock) {
        lastSeqNum = databaseSeqNum
        bindings.nativePrepareStatement(connectionPtr, sql)
    }

    override fun entryRemoved(
        evicted: Boolean,
        key: String,
        oldValue: PreparedStatement<SP>,
        newValue: PreparedStatement<SP>?,
    ) {
        oldValue.inCache = false
        if (!oldValue.inUse) {
            bindings.nativeFinalizeStatement(connectionPtr, oldValue.statementPtr)
        }
    }

    fun dump(): String = buildString {
        append("  Prepared statement cache:\n")
        val cache = snapshot()
        if (cache.isNotEmpty()) {
            cache.entries
                .filter { it.value.inCache }
                .forEachIndexed { i, (sql, statement) ->
                    append(
                        "    $i: statementPtr=${statement.statementPtr}, " +
                                "numParameters=${statement.numParameters}, " +
                                "type=${statement.type}, " +
                                "readOnly=${statement.readOnly}, " +
                                "sql=\"${OperationLog.trimSqlForDisplay(sql)}\"" +
                                "\n",
                    )
                }
        } else {
            append("    <none>\n")
        }
    }
}
