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

import ru.pixnews.wasm.sqlite.open.helper.base.AbstractWindowedCursor
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.CursorWindow
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.math.max

/**
 * A Cursor implementation that exposes results from a query on a [SQLiteDatabase].
 *
 * SQLiteCursor is not internally synchronized so code using a SQLiteCursor from multiple
 * threads should perform its own synchronization when using the SQLiteCursor.
 *
 * @param query The query object for the cursor
 */
internal class SQLiteCursor(
    private val query: SQLiteQuery,
    rootLogger: Logger,
) : AbstractWindowedCursor({ name -> CursorWindow(name, rootLogger) }) {
    private val logger = rootLogger.withTag(TAG)

    /** The names of the columns in the rows  */
    private val columns: List<String> = query.columnNames

    /** The number of rows in the cursor  */
    private var count = NO_COUNT

    /** The number of rows that can fit in the cursor window, 0 if unknown  */
    private var cursorWindowCapacity = 0

    /** A mapping of column names to column indices, to speed up lookups  */
    private val columnNameMap: Map<String, Int> by lazy(NONE) {
        columns.mapIndexed { columnNo, name -> name to columnNo }.toMap()
    }

    /** Used to find out where a cursor was allocated in case it never got released.  */
    private val closeGuard: CloseGuard = CloseGuard.get()

    @Suppress("NO_CORRESPONDING_PROPERTY")
    override var window: CursorWindow?
        get() = super.window
        set(value) {
            super.window = value
            count = NO_COUNT
        }

    override fun onMove(oldPosition: Int, newPosition: Int): Boolean {
        // Make sure the row at newPosition is present in the window
        window.let {
            if ((it == null) || newPosition < it.startPosition || newPosition >= (it.startPosition + it.numRows)) {
                fillWindow(newPosition)
            }
        }

        return true
    }

    override fun getCount(): Int {
        if (count == NO_COUNT) {
            fillWindow(0)
        }
        return count
    }

    private fun fillWindow(requiredPos: Int) {
        createWindow(query.database.path)
        val window = checkNotNull(window)

        try {
            if (count == NO_COUNT) {
                val startPos = cursorPickFillWindowStartPosition(requiredPos, 0)
                count = query.fillWindow(window, startPos, requiredPos, true)
                cursorWindowCapacity = window.numRows

                logger.d { "received count(*) from native_fill_window: $count" }
            } else {
                val startPos = cursorPickFillWindowStartPosition(requiredPos, cursorWindowCapacity)
                query.fillWindow(window, startPos, requiredPos, false)
            }
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            // Close the cursor window if the query failed and therefore will
            // not produce any results.  This helps to avoid accidentally leaking
            // the cursor window if the client does not correctly handle exceptions
            // and fails to close the cursor.
            this.window = null
            throw ex
        }
    }

    override fun getColumnIndex(columnName: String): Int {
        // Hack according to bug 903852
        val cleanColumnName = columnName.substringAfterLast(".")
        return columnNameMap.getOrDefault(cleanColumnName, -1)
    }

    override fun getColumnNames(): Array<String> = columns.toTypedArray<String>()

    override fun close() {
        super.close()
        synchronized(this) {
            query.close()
        }
    }

    /**
     * Release the native resources, if they haven't been released yet.
     */
    override fun finalize() {
        try {
            // if the cursor hasn't been closed yet, close it first
            if (window != null) {
                closeGuard.warnIfOpen()
                close()
            }
        } finally {
            super.finalize()
        }
    }

    companion object {
        const val TAG: String = "SQLiteCursor"
        const val NO_COUNT: Int = -1

        fun cursorPickFillWindowStartPosition(
            cursorPosition: Int,
            cursorWindowCapacity: Int,
        ): Int {
            @Suppress("MagicNumber")
            return max((cursorPosition - cursorWindowCapacity / 3).toDouble(), 0.0).toInt()
        }
    }
}
