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
import android.database.sqlite.SQLiteException
import androidx.core.os.CancellationSignal
import at.released.weh.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.CursorWindow

/**
 * Represents a query that reads the resulting rows into a [SQLiteQuery].
 * This class is used by [SQLiteCursor] and isn't useful itself.
 *
 *
 * This class is not thread-safe.
 *
 */
internal class SQLiteQuery(
    db: SQLiteDatabase,
    query: String,
    bindArgs: List<Any?>,
    private val cancellationSignal: CancellationSignal?,
    rootLogger: Logger,
) : SQLiteProgram(db, query, bindArgs, cancellationSignal) {
    private val logger = rootLogger.withTag("SQLiteQuery")

    /**
     * Reads rows into a buffer.
     *
     * @param window The window to fill into
     * @param startPos The start position for filling the window.
     * @param requiredPos The position of a row that MUST be in the window.
     * If it won't fit, then the query should discard part of what it filled.
     * @param countAllRows True to count all rows that the query would
     * return regardless of whether they fit in the window.
     * @return Number of rows that were enumerated.  Might not be all rows
     * unless countAllRows is true.
     * @throws SQLiteException if an error occurs.
     * @throws OperationCanceledException if the operation was canceled.
     * @throws SQLiteDatabaseCorruptException
     */
    fun fillWindow(
        window: CursorWindow,
        startPos: Int,
        requiredPos: Int,
        countAllRows: Boolean,
    ): Int = useReference {
        try {
            session.executeForCursorWindow(
                sql = sql,
                bindArgs = bindArgs,
                window = window,
                startPos = startPos,
                requiredPos = requiredPos,
                countAllRows = countAllRows,
                connectionFlags = connectionFlags,
                cancellationSignal = cancellationSignal,
            )
        } catch (ex: SQLiteDatabaseCorruptException) {
            onCorruption()
            throw ex
        } catch (ex: SQLiteException) {
            logger.e { "exception: ${ex.message}; query: $sql" }
            throw ex
        }
    }

    override fun toString(): String = "SQLiteQuery: $sql"
}
