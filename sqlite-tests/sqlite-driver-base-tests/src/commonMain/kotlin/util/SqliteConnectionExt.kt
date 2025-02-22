/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.test.base.util

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
public inline fun <T : SQLiteConnection, R> T.use(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    var exception: Throwable? = null
    try {
        return block(this)
    } catch (@Suppress("TooGenericExceptionCaught") ex: Throwable) {
        exception = ex
        throw ex
    } finally {
        this.closeFinally(exception)
    }
}

@PublishedApi
internal fun SQLiteConnection.closeFinally(cause: Throwable?): Unit = if (cause == null) {
    close()
} else {
    try {
        close()
    } catch (@Suppress("TooGenericExceptionCaught") closeException: Throwable) {
        cause.addSuppressed(closeException)
    }
}

public fun SQLiteConnection.execSQL(
    sql: String,
    vararg bindArgs: Any?,
): Boolean = prepare(sql).use { statement ->
    statement.bindArgs(bindArgs.toList())
    statement.step()
}

public fun SQLiteConnection.queryForString(
    sql: String,
    vararg bindArgs: Any?,
): String? {
    return queryForSingleResult(sql, SQLiteStatement::getText, bindArgs = bindArgs)
}

public fun SQLiteConnection.queryForLong(
    sql: String,
    vararg bindArgs: Any?,
): Long? {
    return queryForSingleResult(sql, SQLiteStatement::getLong, bindArgs = bindArgs)
}

@Suppress("LAMBDA_IS_NOT_LAST_PARAMETER")
private fun <R : Any> SQLiteConnection.queryForSingleResult(
    sql: String,
    resultFetcher: (SQLiteStatement, Int) -> R?,
    vararg bindArgs: Any?,
): R? = prepare(sql).use { statement ->
    statement.bindArgs(bindArgs.toList())
    if (!statement.step()) {
        error("No row")
    }
    val columnCount = statement.getColumnCount()
    if (columnCount != 1) {
        error("Received `$columnCount` columns when 1 was expected ")
    }
    return if (!statement.isNull(0)) {
        resultFetcher(statement, 0)
    } else {
        null
    }
}

public fun SQLiteConnection.queryTable(
    sql: String,
    vararg bindArgs: Any?,
): List<Map<String, String?>> = prepare(sql).use { statement ->
    statement.bindArgs(bindArgs.toList())
    statement.readResult()
}
