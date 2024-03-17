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
import co.touchlab.kermit.Logger
import ru.pixnews.wasm.sqlite.open.helper.base.CursorWindow
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteProgram.Companion.bindAllArgsAsStrings
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3ConnectionPtr
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3StatementPtr
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3WindowPtr

/**
 * A cursor driver that uses the given query directly.
 */
internal class SQLiteDirectCursorDriver<CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr, WP : Sqlite3WindowPtr>(
    private val database: SQLiteDatabase<CP, SP, WP>,
    private val sql: String,
    private val cancellationSignal: CancellationSignal?,
    private val cursorWindowCtor: (name: String?) -> CursorWindow<WP>,
    logger: Logger,
) : SQLiteCursorDriver<CP, SP, WP> {
    private val logger: Logger = logger.withTag("SQLiteDirectCursorDriver")
    private var query: SQLiteQuery<WP>? = null

    override fun query(
        factory: SQLiteDatabase.CursorFactory<CP, SP, WP>?,
        bindArgs: List<Any?>,
    ): Cursor {
        val query = SQLiteQuery(database, sql, bindArgs, cancellationSignal)
        val cursor: Cursor
        try {
            cursor = factory?.newCursor(database, this, query) ?: SQLiteCursor(
                this,
                query,
                cursorWindowCtor,
                logger,
            )
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            query.close()
            throw ex
        }

        this.query = query
        return cursor
    }

    override fun cursorClosed() {
        // Do nothing
    }

    override fun setBindArguments(bindArgs: List<String?>) {
        requireNotNull(query) { "query() not called" }.bindAllArgsAsStrings(bindArgs)
    }

    override fun cursorDeactivated() {
        // Do nothing
    }

    override fun cursorRequeried(cursor: Cursor) {
        // Do nothing
    }

    override fun toString(): String = "SQLiteDirectCursorDriver: $sql"
}
