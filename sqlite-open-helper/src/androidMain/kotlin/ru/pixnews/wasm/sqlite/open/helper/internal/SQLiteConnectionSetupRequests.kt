/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal

import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteException
import ru.pixnews.wasm.sqlite.open.helper.exception.AndroidSqliteException
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3ConnectionPtr
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3StatementPtr

internal fun <CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr> SQLiteConnection<CP, SP>.setPageSize(
    newSize: Long = SQLiteGlobal.defaultPageSize.toLong(),
) {
    val value = executeForLong("PRAGMA page_size")
    if (value != newSize) {
        execute("PRAGMA page_size=$newSize")
    }
}

internal fun <CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr> SQLiteConnection<CP, SP>.setForeignKeyMode(
    foreignKeyConstraintsEnabled: Boolean,
) {
    val newValue = if (foreignKeyConstraintsEnabled) 1L else 0L
    val value = executeForLong("PRAGMA foreign_keys")
    if (value != newValue) {
        execute("PRAGMA foreign_keys=$newValue")
    }
}

internal fun <CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr> SQLiteConnection<CP, SP>.setJournalSizeLimit(
    newLimit: Long = SQLiteGlobal.journalSizeLimit.toLong(),
) {
    val value = executeForLong("PRAGMA journal_size_limit")
    if (value != newLimit) {
        executeForLong("PRAGMA journal_size_limit=$newLimit")
    }
}

internal fun <CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr> SQLiteConnection<CP, SP>.setAutoCheckpointInterval(
    newInterval: Long = SQLiteGlobal.wALAutoCheckpoint.toLong(),
) {
    val value = executeForLong("PRAGMA wal_autocheckpoint")
    if (value != newInterval) {
        executeForLong("PRAGMA wal_autocheckpoint=$newInterval")
    }
}

internal fun <CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr> SQLiteConnection<CP, SP>.setWalMode(
    isWalEnabled: Boolean,
) {
    if (isWalEnabled) {
        setJournalMode("WAL")
        setSyncMode(SQLiteGlobal.wALSyncMode)
    } else {
        setJournalMode(SQLiteGlobal.defaultJournalMode)
        setSyncMode(SQLiteGlobal.defaultSyncMode)
    }
}

internal fun <CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr> SQLiteConnection<CP, SP>.setSyncMode(
    newValue: String,
) {
    val value = executeForString("PRAGMA synchronous")
    if (!canonicalizeSyncMode(value).equals(canonicalizeSyncMode(newValue), ignoreCase = true)) {
        execute("PRAGMA synchronous=$newValue")
    }
}

internal fun <CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr> SQLiteConnection<CP, SP>.setJournalMode(
    newValue: String,
) {
    val value = executeForString("PRAGMA journal_mode")
    if (!value.equals(newValue, ignoreCase = true)) {
        try {
            val result = executeForString("PRAGMA journal_mode=$newValue")
            if (result.equals(newValue, ignoreCase = true)) {
                return
            }
            // PRAGMA journal_mode silently fails and returns the original journal
            // mode in some cases if the journal mode could not be changed.
        } catch (ex: AndroidSqliteException) {
            // This error (SQLITE_BUSY) occurs if one connection has the database
            // open in WAL mode and another tries to change it to non-WAL.
            if (@Suppress("TooGenericExceptionCaught") ex !is SQLiteDatabaseLockedException) {
                throw ex
            }
        }

        // Because we always disable WAL mode when a database is first opened
        // (even if we intend to re-enable it), we can encounter problems if
        // there is another open connection to the database somewhere.
        // This can happen for a variety of reasons such as an application opening
        // the same database in multiple processes at the same time or if there is a
        // crashing content provider service that the ActivityManager has
        // removed from its registry but whose process hasn't quite died yet
        // by the time it is restarted in a new process.
        //
        // If we don't change the journal mode, nothing really bad happens.
        // In the worst case, an application that enables WAL might not actually
        // get it, although it can still use connection pooling.
        logger.w {
            "Could not change the database journal mode of '" +
                    databaseLabel + "' from '" + value + "' to '" + newValue +
                    "' because the database is locked.  This usually means that " +
                    "there are other open connections to the database which prevents " +
                    "the database from enabling or disabling write-ahead logging mode.  " +
                    "Proceeding without changing the journal mode."
        }
    }
}

internal fun <CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr> SQLiteConnection<CP, SP>.setLocale(
    newLocale: String,
) {
     // TODO: Register the localized collators.
     nativeRegisterLocalizedCollators(newLocale)

    try {
        // Ensure the android metadata table exists.
        execute("CREATE TABLE IF NOT EXISTS android_metadata (locale TEXT)")

        // Check whether the locale was actually changed.
        val oldLocale = executeForString(
            "SELECT locale FROM android_metadata UNION SELECT NULL ORDER BY locale DESC LIMIT 1",
        )
        if (oldLocale == newLocale) {
            return
        }

        // Go ahead and update the indexes using the new locale.
        execute("BEGIN")
        var success = false
        try {
            execute("DELETE FROM android_metadata")
            execute("INSERT INTO android_metadata (locale) VALUES(?)", listOf(newLocale))
            execute("REINDEX LOCALIZED")
            success = true
        } finally {
            execute(if (success) "COMMIT" else "ROLLBACK")
        }
    } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
        throw SQLiteException("Failed to change locale for db '$databaseLabel' to '$newLocale'.", ex)
    }
}

private fun canonicalizeSyncMode(value: String?): String = when (value) {
    "0" -> "OFF"
    "1" -> "NORMAL"
    "2" -> "FULL"
    else -> value.toString()
}
