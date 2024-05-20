/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal

/*
 * Original Copyrights:
 * Copyright (C) 2017-2024 requery.io
 * Copyright (C) 2005-2012 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

import android.database.sqlite.SQLiteDatabaseCorruptException
import androidx.sqlite.db.SupportSQLiteStatement

/**
 * Represents a statement that can be executed against a database.  The statement
 * cannot return multiple rows or columns, but single value (1 x 1) result sets
 * are supported.
 *
 *
 * This class is not thread-safe.
 *
 */
internal class SQLiteStatement(
    db: SQLiteDatabase,
    sql: String,
    bindArgs: List<Any?> = emptyList<Unit>(),
) : SQLiteProgram(db, sql, bindArgs, null), SupportSQLiteStatement {
    /**
     * Execute this SQL statement, if it is not a SELECT / INSERT / DELETE / UPDATE, for example
     * CREATE / DROP table, view, trigger, index etc.
     *
     * @throws SQLException If the SQL string is invalid for some reason
     */
    override fun execute() = executeHandleCorruption {
        session.execute(sql, bindArgs, connectionFlags, null)
    }

    /**
     * Execute this SQL statement, if the number of rows affected by execution of this SQL
     * statement is of any importance to the caller - for example, UPDATE / DELETE SQL statements.
     *
     * @return the number of rows affected by this SQL statement execution.
     * @throws SQLException If the SQL string is invalid for some reason
     */
    override fun executeUpdateDelete(): Int = executeHandleCorruption {
        return session.executeForChangedRowCount(sql, bindArgs, connectionFlags, null)
    }

    /**
     * Execute this SQL statement and return the ID of the row inserted due to this call.
     * The SQL statement should be an INSERT for this to be a useful call.
     *
     * @return the row ID of the last row inserted, if this insert is successful. -1 otherwise.
     * @throws SQLException If the SQL string is invalid for some reason
     */
    override fun executeInsert(): Long = executeHandleCorruption {
        session.executeForLastInsertedRowId(sql, bindArgs, connectionFlags, null)
    }

    /**
     * Execute a statement that returns a 1 by 1 table with a numeric value.
     * For example, SELECT COUNT(*) FROM table;
     *
     * @return The result of the query.
     * @throws SQLiteDoneException if the query returns zero rows
     */
    override fun simpleQueryForLong(): Long = executeHandleCorruption {
        session.executeForLong(sql, bindArgs, connectionFlags, null)
    }

    /**
     * Execute a statement that returns a 1 by 1 table with a text value.
     * For example, SELECT COUNT(*) FROM table;
     *
     * @return The result of the query.
     * @throws SQLiteDoneException if the query returns zero rows
     */
    override fun simpleQueryForString(): String? = executeHandleCorruption {
        session.executeForString(sql, bindArgs, connectionFlags, null)
    }

    private inline fun <R : Any?> executeHandleCorruption(block: () -> R): R = useReference {
        try {
            return block()
        } catch (ex: SQLiteDatabaseCorruptException) {
            onCorruption()
            throw ex
        }
    }

    override fun toString(): String = "SQLiteProgram: $sql"
}
