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
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteDatabase.CursorFactory
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3ConnectionPtr
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3StatementPtr

/**
 * A driver for SQLiteCursors that is used to create them and gets notified
 * by the cursors it creates on significant events in their lifetimes.
 */
internal interface SQLiteCursorDriver<CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr> {
    /**
     * Executes the query returning a Cursor over the result set.
     *
     * @param factory The CursorFactory to use when creating the Cursors, or
     * null if standard SQLiteCursors should be returned.
     * @return a Cursor over the result set
     */
    fun query(factory: CursorFactory<CP, SP>?, bindArgs: List<Any?>): Cursor

    /**
     * Called by a SQLiteCursor when it is released.
     */
    fun cursorDeactivated()

    /**
     * Called by a SQLiteCursor when it is requeried.
     */
    fun cursorRequeried(cursor: Cursor)

    /**
     * Called by a SQLiteCursor when it it closed to destroy this object as well.
     */
    fun cursorClosed()

    /**
     * Set new bind arguments. These will take effect in cursorRequeried().
     *
     * @param bindArgs the new arguments
     */
    fun setBindArguments(bindArgs: List<String?>)
}
