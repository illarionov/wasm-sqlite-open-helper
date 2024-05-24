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

import androidx.core.os.CancellationSignal
import androidx.sqlite.db.SupportSQLiteProgram
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteDatabase.Companion.getThreadDefaultConnectionFlags
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteStatementType.STATEMENT_ABORT
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteStatementType.STATEMENT_BEGIN
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteStatementType.STATEMENT_COMMIT

/**
 * A base class for compiled SQLite programs.
 *
 *
 * This class is not thread-safe.
 *
 */
internal abstract class SQLiteProgram internal constructor(
    val database: SQLiteDatabase,
    sql: String,
    bindArgs: List<Any?>,
    cancellationSignalForPrepare: CancellationSignal?,
) : SQLiteClosable(), SupportSQLiteProgram {
    val sql: String = sql.trim { it <= ' ' }
    val columnNames: List<String>
    private val readOnly: Boolean
    private val numParameters: Int
    private val _bindArgs: MutableList<Any?>
    val bindArgs: List<Any?> get() = _bindArgs

    protected val session: SQLiteSession
        get() = database.threadSession

    protected val connectionFlags: Int
        get() = getThreadDefaultConnectionFlags(readOnly)

    init {
        when (val statementType = SQLiteStatementType.getSqlStatementType(this.sql)) {
            STATEMENT_BEGIN, STATEMENT_COMMIT, STATEMENT_ABORT -> {
                readOnly = false
                columnNames = listOf()
                numParameters = 0
            }

            else -> {
                val assumeReadOnly = (statementType == SQLiteStatementType.STATEMENT_SELECT)
                val info = database.threadSession.prepare(
                    this.sql,
                    getThreadDefaultConnectionFlags(assumeReadOnly),
                    cancellationSignalForPrepare,
                )
                readOnly = info.readOnly
                columnNames = info.columnNames
                numParameters = info.numParameters
            }
        }

        require(bindArgs.size <= numParameters) {
            "Too many bind arguments. ${bindArgs.size} arguments were provided but the statement " +
                    "needs $numParameters arguments."
        }

        this._bindArgs = MutableList(numParameters) { null }
        bindArgs.forEachIndexed { index, value -> _bindArgs[index] = value }
    }

    protected fun onCorruption(): Unit = database.onCorruption()

    override fun bindNull(index: Int) = bind(index, null)

    override fun bindLong(index: Int, value: Long) = bind(index, value)

    override fun bindDouble(index: Int, value: Double) = bind(index, value)

    override fun bindString(index: Int, value: String) = bind(index, value)

    override fun bindBlob(index: Int, value: ByteArray) = bind(index, value)

    override fun clearBindings() = _bindArgs.indices.forEach { _bindArgs[it] = null }

    override fun onAllReferencesReleased() = clearBindings()

    private fun bind(index: Int, value: Any?) {
        require(index in 1..numParameters) {
            "Cannot bind argument at index " +
                    index + " because the index is out of range.  " +
                    "The statement has " + numParameters + " parameters."
        }
        _bindArgs[index - 1] = value
    }

    companion object {
        /**
         * Given an array of String bindArgs, this method binds all of them in one single call.
         *
         * @param bindArgs the String array of bind args, none of which must be null.
         */
        fun SupportSQLiteProgram.bindAllArgsAsStrings(bindArgs: List<String?>) {
            for (i in bindArgs.size downTo 1) {
                val arg = bindArgs[i - 1]
                if (arg != null) {
                    bindString(i, arg)
                } else {
                    bindNull(i)
                }
            }
        }
    }
}
