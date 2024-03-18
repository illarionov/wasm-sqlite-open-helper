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

import android.database.Cursor
import androidx.core.os.CancellationSignal
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteProgram.Companion.bindAllArgsAsStrings
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3ConnectionPtr
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3StatementPtr

/**
 * A cursor driver that uses the given query directly.
 */
internal class SQLiteDirectCursorDriver<CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr>(
    private val database: SQLiteDatabase<CP, SP>,
    private val sql: String,
    private val cancellationSignal: CancellationSignal?,
    rootLogger: Logger,
) : SQLiteCursorDriver<CP, SP> {
    private val logger: Logger = rootLogger.withTag("SQLiteDirectCursorDriver")
    private var query: SQLiteQuery? = null

    override fun query(
        factory: SQLiteDatabase.CursorFactory<CP, SP>?,
        bindArgs: List<Any?>,
    ): Cursor {
        val query = SQLiteQuery(database, sql, bindArgs, cancellationSignal)
        val cursor: Cursor
        try {
            cursor = factory?.newCursor(database, this, query) ?: SQLiteCursor(query, logger)
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            query.close()
            throw ex
        }

        this.query = query
        return cursor
    }

    override fun setBindArguments(bindArgs: List<String?>) {
        requireNotNull(query) { "query() not called" }.bindAllArgsAsStrings(bindArgs)
    }

    override fun toString(): String = "SQLiteDirectCursorDriver: $sql"
}
