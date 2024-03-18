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

import android.annotation.SuppressLint
import android.database.Cursor
import android.database.sqlite.SQLiteBindOrColumnIndexOutOfRangeException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteException
import android.util.Printer
import androidx.collection.LruCache
import androidx.core.os.CancellationSignal
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion.ENABLE_WRITE_AHEAD_LOGGING
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion.OPEN_READONLY
import ru.pixnews.wasm.sqlite.open.helper.SqliteDatabaseConfiguration
import ru.pixnews.wasm.sqlite.open.helper.base.CursorWindow
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.contains
import ru.pixnews.wasm.sqlite.open.helper.common.api.xor
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteStatementType.STATEMENT_SELECT
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteStatementType.STATEMENT_UPDATE
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.SqlOpenHelperNativeBindings
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3ConnectionPtr
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3StatementPtr
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.isNotNull
import ru.pixnews.wasm.sqlite.open.helper.toSqliteOpenFlags
import java.text.SimpleDateFormat
import java.util.Date
import java.util.regex.Pattern

/**
 * Represents a SQLite database connection.
 * Each connection wraps an instance of a native `sqlite3` object.
 *
 * When database connection pooling is enabled, there can be multiple active
 * connections to the same database.  Otherwise there is typically only one
 * connection per database.
 *
 * When the SQLite WAL feature is enabled, multiple readers and one writer
 * can concurrently access the database.  Without WAL, readers and writers
 * are mutually exclusive.
 *
 * <h2>Ownership and concurrency guarantees</h2>
 *
 * Connection objects are not thread-safe.  They are acquired as needed to
 * perform a database operation and are then returned to the pool.  At any
 * given time, a connection is either owned and used by a [SQLiteSession]
 * object or the [SQLiteConnectionPool].  Those classes are
 * responsible for serializing operations to guard against concurrent
 * use of a connection.
 *
 * The guarantee of having a single owner allows this class to be implemented
 * without locks and greatly simplifies resource management.
 *
 * <h2>Encapsulation guarantees</h2>
 *
 * The connection object owns *all* of the SQLite related native
 * objects that are associated with the connection.  What's more, there are
 * no other objects in the system that are capable of obtaining handles to
 * those native objects.  Consequently, when the connection is closed, we do
 * not have to worry about what other components might have references to
 * its associated SQLite state -- there are none.
 *
 * Encapsulation is what ensures that the connection object's
 * lifecycle does not become a tortured mess of finalizers and reference
 * queues.
 *
 * <h2>Reentrance</h2>
 *
 * This class must tolerate reentrant execution of SQLite operations because
 * triggers may call custom SQLite functions that perform additional queries.
 *
 */
@Suppress("LargeClass")
internal class SQLiteConnection<CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr> private constructor(
    private val pool: SQLiteConnectionPool<CP, SP>,
    configuration: SqliteDatabaseConfiguration,
    private val bindings: SqlOpenHelperNativeBindings<CP, SP>,
    private val connectionId: Int,
    internal val isPrimaryConnection: Boolean,
    private val debugConfig: SQLiteDebug,
    rootLogger: Logger,
) : CancellationSignal.OnCancelListener {
    private val logger = rootLogger.withTag(TAG)
    private val closeGuard: CloseGuard = CloseGuard.get()
    private val configuration = SqliteDatabaseConfiguration(configuration)
    private val isReadOnlyConnection = configuration.openFlags.contains(OPEN_READONLY)
    private val preparedStatementCache = PreparedStatementCache(this.configuration.maxSqlCacheSize)

    // The recent operations log.
    private val recentOperations = OperationLog(debugConfig, logger)

    // The native SQLiteConnection pointer.  (FOR INTERNAL USE ONLY)
    private var connectionPtr: CP = bindings.connectionNullPtr()
    private var onlyAllowReadOnlyOperations = false

    // The number of times attachCancellationSignal has been called.
    // Because SQLite statement execution can be reentrant, we keep track of how many
    // times we have attempted to attach a cancellation signal to the connection so that
    // we can ensure that we detach the signal at the right time.
    private var cancellationSignalAttachCount = 0

    init {
        closeGuard.open("close")
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        if (connectionPtr.isNotNull()) {
            pool.onConnectionLeaked()
        }

        dispose(true)
    }

    // Called by SQLiteConnectionPool only.
    // Closes the database closes and releases all of its associated resources.
    // Do not call methods on the connection after it is closed.  It will probably crash.
    fun close() {
        dispose(false)
    }

    private fun open() {
        connectionPtr = bindings.nativeOpen(
            path = configuration.path,
            openFlags = configuration.openFlags.toSqliteOpenFlags(),
            label = configuration.label,
            enableTrace = debugConfig.sqlStatements,
            enableProfile = debugConfig.sqlTime,
        )

        setPageSize()
        setForeignKeyModeFromConfiguration()
        setJournalSizeLimit()
        setAutoCheckpointInterval()
        setWalModeFromConfiguration()
        setLocaleFromConfiguration()
    }

    private fun dispose(finalized: Boolean) {
        if (finalized) {
            closeGuard.warnIfOpen()
        }
        closeGuard.close()

        if (connectionPtr.isNotNull()) {
            val cookie = recentOperations.beginOperation("close", null)
            try {
                preparedStatementCache.evictAll()
                bindings.nativeClose(connectionPtr)
                connectionPtr = bindings.connectionNullPtr()
            } finally {
                recentOperations.endOperation(cookie)
            }
        }
    }

    private fun setPageSize() {
        if (!configuration.isInMemoryDb && !isReadOnlyConnection) {
            val newValue = SQLiteGlobal.defaultPageSize.toLong()
            val value = executeForLong("PRAGMA page_size")
            if (value != newValue) {
                execute("PRAGMA page_size=$newValue")
            }
        }
    }

    private fun setAutoCheckpointInterval() {
        if (!configuration.isInMemoryDb && !isReadOnlyConnection) {
            val newValue = SQLiteGlobal.wALAutoCheckpoint.toLong()
            val value = executeForLong("PRAGMA wal_autocheckpoint")
            if (value != newValue) {
                executeForLong("PRAGMA wal_autocheckpoint=$newValue")
            }
        }
    }

    private fun setJournalSizeLimit() {
        if (!configuration.isInMemoryDb && !isReadOnlyConnection) {
            val newValue = SQLiteGlobal.journalSizeLimit.toLong()
            val value = executeForLong("PRAGMA journal_size_limit")
            if (value != newValue) {
                executeForLong("PRAGMA journal_size_limit=$newValue")
            }
        }
    }

    private fun setForeignKeyModeFromConfiguration() {
        if (!isReadOnlyConnection) {
            val newValue = (if (configuration.foreignKeyConstraintsEnabled) 1 else 0).toLong()
            val value = executeForLong("PRAGMA foreign_keys")
            if (value != newValue) {
                execute("PRAGMA foreign_keys=$newValue")
            }
        }
    }

    private fun setWalModeFromConfiguration() {
        if (!configuration.isInMemoryDb && !isReadOnlyConnection) {
            if (configuration.openFlags.contains(ENABLE_WRITE_AHEAD_LOGGING)) {
                setJournalMode("WAL")
                setSyncMode(SQLiteGlobal.wALSyncMode)
            } else {
                setJournalMode(SQLiteGlobal.defaultJournalMode)
                setSyncMode(SQLiteGlobal.defaultSyncMode)
            }
        }
    }

    private fun setSyncMode(newValue: String) {
        val value = executeForString("PRAGMA synchronous")
        if (!canonicalizeSyncMode(value).equals(
                canonicalizeSyncMode(newValue),
                ignoreCase = true,
            )
        ) {
            execute("PRAGMA synchronous=$newValue")
        }
    }

    private fun setJournalMode(newValue: String) {
        val value = executeForString("PRAGMA journal_mode")
        if (!value.equals(newValue, ignoreCase = true)) {
            try {
                val result = executeForString("PRAGMA journal_mode=$newValue")
                if (result.equals(newValue, ignoreCase = true)) {
                    return
                }
                // PRAGMA journal_mode silently fails and returns the original journal
                // mode in some cases if the journal mode could not be changed.
            } catch (ex: SQLiteException) {
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
                        configuration.label + "' from '" + value + "' to '" + newValue +
                        "' because the database is locked.  This usually means that " +
                        "there are other open connections to the database which prevents " +
                        "the database from enabling or disabling write-ahead logging mode.  " +
                        "Proceeding without changing the journal mode."
            }
        }
    }

    private fun setLocaleFromConfiguration() {
        // Register the localized collators.
        val newLocale = configuration.locale.toString()
        // Removed: bindings.nativeRegisterLocalizedCollators(connectionPtr, newLocale)

        // If the database is read-only, we cannot modify the android metadata table
        // or existing indexes.
        if (isReadOnlyConnection) {
            return
        }

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
            throw SQLiteException("Failed to change locale for db '${configuration.label}' to '$newLocale'.", ex)
        }
    }

    // Called by SQLiteConnectionPool only.
    fun reconfigure(newConfiguration: SqliteDatabaseConfiguration) {
        onlyAllowReadOnlyOperations = false

        // Remember what changed.
        val foreignKeyModeChanged = newConfiguration.foreignKeyConstraintsEnabled !=
                this.configuration.foreignKeyConstraintsEnabled
        val walModeChanged =
            (newConfiguration.openFlags xor this.configuration.openFlags).contains(ENABLE_WRITE_AHEAD_LOGGING)
        val localeChanged = newConfiguration.locale != this.configuration.locale

        // Update configuration parameters.
        this.configuration.updateParametersFrom(newConfiguration)

        // Update prepared statement cache size.
        /* mPreparedStatementCache.resize(configuration.maxSqlCacheSize); */

        // Update foreign key mode.
        if (foreignKeyModeChanged) {
            setForeignKeyModeFromConfiguration()
        }

        // Update WAL.
        if (walModeChanged) {
            setWalModeFromConfiguration()
        }

        // Update locale.
        if (localeChanged) {
            setLocaleFromConfiguration()
        }
    }

    // Called by SQLiteConnectionPool only.
    // When set to true, executing write operations will throw SQLiteException.
    // Preparing statements that might write is ok, just don't execute them.
    internal fun setOnlyAllowReadOnlyOperations(readOnly: Boolean) {
        onlyAllowReadOnlyOperations = readOnly
    }

    // Called by SQLiteConnectionPool only.
    // Returns true if the prepared statement cache contains the specified SQL.
    internal fun isPreparedStatementInCache(sql: String): Boolean = preparedStatementCache[sql] != null

    /**
     * Prepares a statement for execution but does not bind its parameters or execute it.
     *
     *
     * This method can be used to check for syntax errors during compilation
     * prior to execution of the statement.  If the `outStatementInfo` argument
     * is not null, the provided [SQLiteStatementInfo] object is populated
     * with information about the statement.
     *
     *
     * A prepared statement makes no reference to the arguments that may eventually
     * be bound to it, consequently it it possible to cache certain prepared statements
     * such as SELECT or INSERT/UPDATE statements.  If the statement is cacheable,
     * then it will be stored in the cache for later.
     *
     * To take advantage of this behavior as an optimization, the connection pool
     * provides a method to acquire a connection that already has a given SQL statement
     * in its prepared statement cache so that it is ready for execution.
     *
     * @param sql The SQL statement to prepare.
     * @param outStatementInfo The[SQLiteStatementInfo] object to populate
     * with information about the statement, or null if none.
     * @throws SQLiteException if an error occurs, such as a syntax error.
     * @throws RuntimeException
     */
    fun prepare(sql: String): SQLiteStatementInfo {
        val cookie = recentOperations.beginOperation("prepare", sql)
        try {
            // TODO: inline func
            val statement = acquirePreparedStatement(sql)
            try {
                val columnCount = bindings.nativeGetColumnCount(connectionPtr, statement.statementPtr)
                return SQLiteStatementInfo(
                    numParameters = statement.numParameters,
                    readOnly = statement.readOnly,
                    columnNames = List(columnCount) { columnNo ->
                        checkNotNull(bindings.nativeGetColumnName(connectionPtr, statement.statementPtr, columnNo)) {
                            "Column $columnNo not found"
                        }
                    },
                )
            } finally {
                releasePreparedStatement(statement)
            }
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            recentOperations.failOperation(cookie, ex)
            throw ex
        } finally {
            recentOperations.endOperation(cookie)
        }
    }

    /**
     * Executes a statement that does not return a result.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     * @throws RuntimeException
     */
    fun execute(
        sql: String,
        bindArgs: List<Any?> = listOf(),
        cancellationSignal: CancellationSignal? = null,
    ) {
        val cookie = recentOperations.beginOperation("execute", sql, bindArgs)
        try {
            val statement = acquirePreparedStatement(sql)
            try {
                throwIfStatementForbidden(statement)
                bindArguments(statement, bindArgs)
                attachCancellationSignal(cancellationSignal)
                try {
                    bindings.nativeExecute(connectionPtr, statement.statementPtr)
                } finally {
                    detachCancellationSignal(cancellationSignal)
                }
            } finally {
                releasePreparedStatement(statement)
            }
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            recentOperations.failOperation(cookie, ex)
            throw ex
        } finally {
            recentOperations.endOperation(cookie)
        }
    }

    /**
     * Executes a statement that returns a single `long` result.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The value of the first column in the first row of the result set
     * as a `long`, or zero if none.
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     * @throws RuntimeException
     */
    fun executeForLong(
        sql: String,
        bindArgs: List<Any?> = emptyList(),
        cancellationSignal: CancellationSignal? = null,
    ): Long {
        val cookie = recentOperations.beginOperation("executeForLong", sql, bindArgs)
        try {
            val statement = acquirePreparedStatement(sql)
            try {
                throwIfStatementForbidden(statement)
                bindArguments(statement, bindArgs)
                attachCancellationSignal(cancellationSignal)
                try {
                    return bindings.nativeExecuteForLong(connectionPtr, statement.statementPtr)
                } finally {
                    detachCancellationSignal(cancellationSignal)
                }
            } finally {
                releasePreparedStatement(statement)
            }
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            recentOperations.failOperation(cookie, ex)
            throw ex
        } finally {
            recentOperations.endOperation(cookie)
        }
    }

    /**
     * Executes a statement that returns a single [String] result.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The value of the first column in the first row of the result set
     * as a `String`, or null if none.
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     * @throws RuntimeException
     */
    fun executeForString(
        sql: String,
        bindArgs: List<Any?> = listOf(),
        cancellationSignal: CancellationSignal? = null,
    ): String? {
        val cookie = recentOperations.beginOperation("executeForString", sql, bindArgs)
        try {
            val statement = acquirePreparedStatement(sql)
            try {
                throwIfStatementForbidden(statement)
                bindArguments(statement, bindArgs)
                attachCancellationSignal(cancellationSignal)
                try {
                    return bindings.nativeExecuteForString(connectionPtr, statement.statementPtr)
                } finally {
                    detachCancellationSignal(cancellationSignal)
                }
            } finally {
                releasePreparedStatement(statement)
            }
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            recentOperations.failOperation(cookie, ex)
            throw ex
        } finally {
            recentOperations.endOperation(cookie)
        }
    }

    /**
     * Executes a statement that returns a count of the number of rows
     * that were changed.  Use for UPDATE or DELETE SQL statements.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The number of rows that were changed.
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     * @throws RuntimeException
     */
    fun executeForChangedRowCount(
        sql: String,
        bindArgs: List<Any?> = listOf(),
        cancellationSignal: CancellationSignal? = null,
    ): Int {
        var changedRows = 0
        val cookie = recentOperations.beginOperation("executeForChangedRowCount", sql, bindArgs)
        try {
            val statement = acquirePreparedStatement(sql)
            try {
                throwIfStatementForbidden(statement)
                bindArguments(statement, bindArgs)
                attachCancellationSignal(cancellationSignal)
                try {
                    changedRows = bindings.nativeExecuteForChangedRowCount(connectionPtr, statement.statementPtr)
                    return changedRows
                } finally {
                    detachCancellationSignal(cancellationSignal)
                }
            } finally {
                releasePreparedStatement(statement)
            }
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            recentOperations.failOperation(cookie, ex)
            throw ex
        } finally {
            if (recentOperations.endOperationDeferLog(cookie)) {
                recentOperations.logOperation(cookie, "changedRows=$changedRows")
            }
        }
    }

    /**
     * Executes a statement that returns the row id of the last row inserted
     * by the statement.  Use for INSERT SQL statements.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The row id of the last row that was inserted, or 0 if none.
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     * @throws RuntimeException
     */
    fun executeForLastInsertedRowId(
        sql: String,
        bindArgs: List<Any?> = listOf(),
        cancellationSignal: CancellationSignal? = null,
    ): Long {
        val cookie = recentOperations.beginOperation(
            "executeForLastInsertedRowId",
            sql,
            bindArgs,
        )
        try {
            val statement = acquirePreparedStatement(sql)
            try {
                throwIfStatementForbidden(statement)
                bindArguments(statement, bindArgs)
                attachCancellationSignal(cancellationSignal)
                try {
                    return bindings.nativeExecuteForLastInsertedRowId(connectionPtr, statement.statementPtr)
                } finally {
                    detachCancellationSignal(cancellationSignal)
                }
            } finally {
                releasePreparedStatement(statement)
            }
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            recentOperations.failOperation(cookie, ex)
            throw ex
        } finally {
            recentOperations.endOperation(cookie)
        }
    }

    /**
     * Executes a statement and populates the specified [CursorWindow]
     * with a range of results.  Returns the number of rows that were counted
     * during query execution.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param window The cursor window to clear and fill.
     * @param startPos The start position for filling the window.
     * @param requiredPos The position of a row that MUST be in the window.
     * If it won't fit, then the query should discard part of what it filled
     * so that it does.  Must be greater than or equal to `startPos`.
     * @param countAllRows True to count all rows that the query would return
     * regagless of whether they fit in the window.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The number of rows that were counted during query execution.  Might
     * not be all rows in the result set unless `countAllRows` is true.
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     * @throws RuntimeException
     */
    fun executeForCursorWindow(
        sql: String,
        bindArgs: List<Any?> = listOf(),
        window: CursorWindow,
        startPos: Int = 0,
        requiredPos: Int = 0,
        countAllRows: Boolean = false,
        cancellationSignal: CancellationSignal? = null,
    ): Int {
        window.acquireReference()
        try {
            var actualPos = -1
            var countedRows = -1
            var filledRows = -1
            val cookie = recentOperations.beginOperation("executeForCursorWindow", sql, bindArgs)
            try {
                val statement = acquirePreparedStatement(sql)
                try {
                    throwIfStatementForbidden(statement)
                    bindArguments(statement, bindArgs)
                    attachCancellationSignal(cancellationSignal)
                    try {
                        val result = bindings.nativeExecuteForCursorWindow(
                            connectionPtr = connectionPtr,
                            statementPtr = statement.statementPtr,
                            window = window.windowPtr!!,
                            startPos = startPos,
                            requiredPos = requiredPos,
                            countAllRows = countAllRows,
                        )
                        @Suppress("MagicNumber")
                        actualPos = (result shr 32).toInt()
                        countedRows = result.toInt()
                        filledRows = window.numRows
                        window.startPosition = actualPos
                        return countedRows
                    } finally {
                        detachCancellationSignal(cancellationSignal)
                    }
                } finally {
                    releasePreparedStatement(statement)
                }
            } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
                recentOperations.failOperation(cookie, ex)
                throw ex
            } finally {
                if (recentOperations.endOperationDeferLog(cookie)) {
                    recentOperations.logOperation(
                        cookie,
                        "window='" + window +
                                "', startPos=" + startPos +
                                ", actualPos=" + actualPos +
                                ", filledRows=" + filledRows +
                                ", countedRows=" + countedRows,
                    )
                }
            }
        } finally {
            window.releaseReference()
        }
    }

    private fun acquirePreparedStatement(sql: String): PreparedStatement<SP> {
        var statement = preparedStatementCache[sql]
        var skipCache = false
        if (statement != null) {
            if (!statement.inUse) {
                return statement
            }
            // The statement is already in the cache but is in use (this statement appears
            // to be not only re-entrant but recursive!).  So prepare a new copy of the
            // statement but do not cache it.
            skipCache = true
        }

        val statementPtr = bindings.nativePrepareStatement(connectionPtr, sql)
        try {
            val statementType = SQLiteStatementType.getSqlStatementType(sql)
            val putInCache = !skipCache && isCacheable(statementType)
            statement = PreparedStatement(
                sql = sql,
                statementPtr = statementPtr,
                numParameters = bindings.nativeGetParameterCount(connectionPtr, statementPtr),
                type = statementType,
                readOnly = bindings.nativeIsReadOnly(connectionPtr, statementPtr),
            )
            if (putInCache) {
                preparedStatementCache.put(sql, statement)
                statement.inCache = true
            }
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            // Finalize the statement if an exception occurred and we did not add
            // it to the cache.  If it is already in the cache, then leave it there.
            if (statement == null || !statement.inCache) {
                bindings.nativeFinalizeStatement(connectionPtr, statementPtr)
            }
            throw ex
        }
        statement.inUse = true
        return statement
    }

    private fun releasePreparedStatement(statement: PreparedStatement<SP>) {
        statement.inUse = false
        if (statement.inCache) {
            try {
                bindings.nativeResetStatementAndClearBindings(connectionPtr, statement.statementPtr)
            } catch (ex: SQLiteException) {
                // The statement could not be reset due to an error.  Remove it from the cache.
                // When remove() is called, the cache will invoke its entryRemoved() callback,
                // which will in turn call finalizePreparedStatement() to finalize and
                // recycle the statement.
                logger.d(ex) {
                    "Could not reset prepared statement due to an exception.  " +
                            "Removing it from the cache.  SQL: " +
                            trimSqlForDisplay(statement.sql)
                }

                preparedStatementCache.remove(statement.sql)
            }
        } else {
            finalizePreparedStatement(statement)
        }
    }

    private fun finalizePreparedStatement(statement: PreparedStatement<SP>) {
        bindings.nativeFinalizeStatement(connectionPtr, statement.statementPtr)
    }

    private fun attachCancellationSignal(cancellationSignal: CancellationSignal?) {
        if (cancellationSignal == null) {
            return
        }

        cancellationSignal.throwIfCanceled()

        cancellationSignalAttachCount += 1
        if (cancellationSignalAttachCount == 1) {
            // Reset cancellation flag before executing the statement.
            bindings.nativeResetCancel(connectionPtr, cancelable = true)

            // After this point, onCancel() may be called concurrently.
            cancellationSignal.setOnCancelListener(this)
        }
    }

    private fun detachCancellationSignal(cancellationSignal: CancellationSignal?) {
        if (cancellationSignal == null) {
            return
        }

        assert(cancellationSignalAttachCount > 0)

        cancellationSignalAttachCount -= 1
        if (cancellationSignalAttachCount == 0) {
            // After this point, onCancel() cannot be called concurrently.
            cancellationSignal.setOnCancelListener(null)

            // Reset cancellation flag after executing the statement.
            bindings.nativeResetCancel(connectionPtr, cancelable = false)
        }
    }

    // CancellationSignal.OnCancelListener callback.
    // This method may be called on a different thread than the executing statement.
    // However, it will only be called between calls to attachCancellationSignal and
    // detachCancellationSignal, while a statement is executing.  We can safely assume
    // that the SQLite connection is still alive.
    override fun onCancel() = bindings.nativeCancel(connectionPtr)

    @Suppress("LongMethod")
    private fun bindArguments(statement: PreparedStatement<SP>, bindArgs: List<Any?>) {
        if (bindArgs.size != statement.numParameters) {
            throw SQLiteBindOrColumnIndexOutOfRangeException(
                "Expected ${statement.numParameters} bind arguments but ${bindArgs.size} were provided.",
            )
        }

        val statementPtr = statement.statementPtr
        bindArgs.forEachIndexed { i, arg ->
            when (getTypeOfObject(arg)) {
                Cursor.FIELD_TYPE_NULL -> bindings.nativeBindNull(
                    connectionPtr = connectionPtr,
                    statementPtr = statementPtr,
                    index = i + 1,
                )

                Cursor.FIELD_TYPE_INTEGER -> bindings.nativeBindLong(
                    connectionPtr = connectionPtr,
                    statementPtr = statementPtr,
                    index = i + 1,
                    value = (arg as Number).toLong(),
                )

                Cursor.FIELD_TYPE_FLOAT -> bindings.nativeBindDouble(
                    connectionPtr = connectionPtr,
                    statementPtr = statementPtr,
                    index = i + 1,
                    value = (arg as Number).toDouble(),
                )

                Cursor.FIELD_TYPE_BLOB -> bindings.nativeBindBlob(
                    connectionPtr = connectionPtr,
                    statementPtr = statementPtr,
                    index = i + 1,
                    value = arg as ByteArray,
                )

                Cursor.FIELD_TYPE_STRING -> when (arg) {
                    // Provide compatibility with legacy applications which may pass
                    // Boolean values in bind args.
                    is Boolean -> bindings.nativeBindLong(
                        connectionPtr = connectionPtr,
                        statementPtr = statementPtr,
                        index = i + 1,
                        value = (if (arg) 1 else 0).toLong(),
                    )

                    else -> bindings.nativeBindString(
                        connectionPtr = connectionPtr,
                        statementPtr = statementPtr,
                        index = i + 1,
                        value = arg.toString(),
                    )
                }

                else -> when (arg) {
                    is Boolean -> bindings.nativeBindLong(
                        connectionPtr = connectionPtr,
                        statementPtr = statementPtr,
                        index = i + 1,
                        value = (if (arg) 1 else 0).toLong(),
                    )

                    else -> bindings.nativeBindString(
                        connectionPtr = connectionPtr,
                        statementPtr = statementPtr,
                        index = i + 1,
                        value = arg.toString(),
                    )
                }
            }
        }
    }

    private fun throwIfStatementForbidden(statement: PreparedStatement<*>) {
        if (onlyAllowReadOnlyOperations && !statement.readOnly) {
            throw SQLiteException(
                "Cannot execute this statement because it might modify the database but the connection is read-only.",
            )
        }
    }

    /**
     * Describes the currently executing operation, in the case where the
     * caller might not actually own the connection.
     *
     * This function is written so that it may be called by a thread that does not
     * own the connection.  We need to be very careful because the connection state is
     * not synchronized.
     *
     * At worst, the method may return stale or slightly wrong data, however
     * it should not crash.  This is ok as it is only used for diagnostic purposes.
     *
     * @return A description of the current operation including how long it has been running,
     * or null if none.
     */
    fun describeCurrentOperationUnsafe(): String? = recentOperations.describeCurrentOperation()

    /**
     * Collects statistics about database connection memory usage.
     *
     * @param dbStatsList The list to populate.
     */
    fun collectDbStats(dbStatsList: ArrayList<DbStats>) {
        // Get information about the main database.
        val lookaside = bindings.nativeGetDbLookaside(connectionPtr)
        var pageCount: Long = 0
        var pageSize: Long = 0
        try {
            pageCount = executeForLong("PRAGMA page_count;")
            pageSize = executeForLong("PRAGMA page_size;")
        } catch (@Suppress("SwallowedException") ex: SQLiteException) {
            // Ignore.
        }
        dbStatsList.add(getMainDbStatsUnsafe(lookaside, pageCount, pageSize))

        // Get information about attached databases.
        // We ignore the first row in the database list because it corresponds to
        // the main database which we have already described.
        val window = CursorWindow("collectDbStats", logger)
        try {
            executeForCursorWindow(
                sql = "PRAGMA database_list;",
                window = window,
            )
            for (i in 1 until window.numRows) {
                val name = window.getString(i, 1) ?: ""
                val path = window.getString(i, 2) ?: ""
                pageCount = 0
                pageSize = 0
                try {
                    pageCount = executeForLong("PRAGMA $name.page_count;")
                    pageSize = executeForLong("PRAGMA $name.page_size;")
                } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") ex: SQLiteException) {
                    // Ignore.
                }
                var label = "  (attached) $name"
                if (path.isNotEmpty()) {
                    label += ": $path"
                }
                dbStatsList.add(DbStats(label, pageCount, pageSize, 0, 0, 0, 0))
            }
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") ex: SQLiteException) {
            // Ignore.
        } finally {
            window.close()
        }
    }

    /**
     * Collects statistics about database connection memory usage, in the case where the
     * caller might not actually own the connection.
     */
    fun collectDbStatsUnsafe(dbStatsList: MutableList<DbStats>) {
        dbStatsList.add(getMainDbStatsUnsafe(0, 0, 0))
    }

    private fun getMainDbStatsUnsafe(lookaside: Int, pageCount: Long, pageSize: Long): DbStats {
        // The prepared statement cache is thread-safe so we can access its statistics
        // even if we do not own the database connection.
        var label = configuration.path
        if (!isPrimaryConnection) {
            label += " ($connectionId)"
        }
        return DbStats(
            dbName = label,
            pageCount = pageCount,
            pageSize = pageSize,
            lookaside = lookaside,
            hits = preparedStatementCache.hitCount(),
            misses = preparedStatementCache.missCount(),
            cachesize = preparedStatementCache.size(),
        )
    }

    override fun toString(): String {
        return "SQLiteConnection: ${configuration.path} ($connectionId)"
    }

    /**
     * Holder type for a prepared statement.
     *
     * Although this object holds a pointer to a native statement object, it
     * does not have a finalizer.  This is deliberate.  The [SQLiteConnection]
     * owns the statement object and will take care of freeing it when needed.
     * In particular, closing the connection requires a guarantee of deterministic
     * resource disposal because all native statement objects must be freed before
     * the native database object can be closed.  So no finalizers here.
     *
     * @property sql The SQL from which the statement was prepared.
     * @property statementPtr Lifetime is managed explicitly by the connection.
     *   The native sqlite3_stmt object pointer.
     * @property numParameters The number of parameters that the prepared statement has.
     * @property type The statement type.
     * @property readOnly True if the statement is read-only.
     * @property inCache True if the statement is in the cache.
     * @property inUse in use statements from being finalized until they are no longer in use.
     *   possible for SQLite calls to be re-entrant.  Consequently we need to prevent
     *   We need this flag because due to the use of custom functions in triggers, it's
     */
    private data class PreparedStatement<SP : Sqlite3StatementPtr>(
        val sql: String,
        val statementPtr: SP,
        val numParameters: Int = 0,
        val type: SQLiteStatementType = STATEMENT_SELECT,
        val readOnly: Boolean = false,
        var inCache: Boolean = false,
        var inUse: Boolean = false,
    )

    private inner class PreparedStatementCache(size: Int) : LruCache<String, PreparedStatement<SP>>(size) {
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: PreparedStatement<SP>,
            newValue: PreparedStatement<SP>?,
        ) {
            oldValue.inCache = false
            if (!oldValue.inUse) {
                finalizePreparedStatement(oldValue)
            }
        }

        fun dump(printer: Printer) {
            printer.println("  Prepared statement cache:")
            val cache = snapshot()
            if (cache.isNotEmpty()) {
                var i = 0
                for ((sql, statement) in cache) {
                    if (statement.inCache) {
// might be false due to a race with entryRemoved
                        printer.println(
                            "    " + i + ": statementPtr=${statement.statementPtr}" +
                                    ", numParameters=" + statement.numParameters +
                                    ", type=" + statement.type +
                                    ", readOnly=" + statement.readOnly +
                                    ", sql=\"" + trimSqlForDisplay(sql) + "\"",
                        )
                    }
                    i += 1
                }
            } else {
                printer.println("    <none>")
            }
        }
    }

    private class OperationLog(
        private val debugConfig: SQLiteDebug,
        rootRogger: Logger,
    ) {
        private val logger = rootRogger.withTag(OperationLog::class.qualifiedName!!)
        private val operations: Array<Operation?> = arrayOfNulls(MAX_RECENT_OPERATIONS)
        private var index: Int = 0
        private var generation: Int = 0

        fun beginOperation(kind: String?, sql: String?, bindArgs: List<Any?> = emptyList()): Int =
            synchronized(operations) {
                val index = (index + 1) % MAX_RECENT_OPERATIONS
                var operation = operations[index]
                if (operation == null) {
                    operation = Operation()
                    operations[index] = operation
                } else {
                    operation.finished = false
                    operation.exception = null
                }
                operation.startTime = System.currentTimeMillis()
                operation.kind = kind
                operation.sql = sql
                operation.bindArgs = bindArgs.map {
                    if (it is ByteArray) {
                        // Don't hold onto the real byte array longer than necessary.
                        arrayOf<Byte>()
                    } else {
                        it
                    }
                }
                operation.cookie = newOperationCookieLocked(index)
                this.index = index
                return operation.cookie
            }

        fun failOperation(cookie: Int, ex: Exception?) = synchronized(operations) {
            val operation = getOperationLocked(cookie)
            if (operation != null) {
                operation.exception = ex
            }
        }

        fun endOperation(cookie: Int) = synchronized(operations) {
            if (endOperationDeferLogLocked(cookie)) {
                logOperationLocked(cookie, null)
            }
        }

        fun endOperationDeferLog(cookie: Int): Boolean = synchronized(operations) {
            return endOperationDeferLogLocked(cookie)
        }

        fun logOperation(cookie: Int, detail: String?) = synchronized(operations) {
            logOperationLocked(cookie, detail)
        }

        private fun endOperationDeferLogLocked(cookie: Int): Boolean {
            val operation = getOperationLocked(cookie)
            if (operation != null) {
                operation.endTime = System.currentTimeMillis()
                operation.finished = true
                return debugConfig.sqlLog && debugConfig.shouldLogSlowQuery(operation.endTime - operation.startTime)
            }
            return false
        }

        private fun logOperationLocked(cookie: Int, detail: String?) {
            val operation = getOperationLocked(cookie) ?: return
            val msg = StringBuilder()
            operation.describe(msg, false)
            if (detail != null) {
                msg.append(", ").append(detail)
            }
            logger.d(message = msg::toString)
        }

        private fun newOperationCookieLocked(index: Int): Int {
            val generation = generation++
            return generation shl COOKIE_GENERATION_SHIFT or index
        }

        private fun getOperationLocked(cookie: Int): Operation? {
            val index = cookie and COOKIE_INDEX_MASK
            val operation = operations[index]
            return if (operation!!.cookie == cookie) operation else null
        }

        fun describeCurrentOperation(): String? = synchronized(operations) {
            val operation = operations[index]
            if (operation != null && !operation.finished) {
                val msg = StringBuilder()
                operation.describe(msg, false)
                return msg.toString()
            }
            return null
        }

        fun dump(printer: Printer, verbose: Boolean) = synchronized(operations) {
            printer.println("  Most recently executed operations:")
            var index = index
            var operation: Operation? = operations[index]
            if (operation != null) {
                var operationNo = 0
                do {
                    val msg = buildString {
                        append("    ")
                        append(operationNo)
                        append(": [")
                        append(operation!!.formattedStartTime)
                        append("] ")
                        operation!!.describe(this, verbose)
                    }
                    printer.println(msg)

                    if (index > 0) {
                        index -= 1
                    } else {
                        index = MAX_RECENT_OPERATIONS - 1
                    }
                    operationNo += 1
                    operation = operations[index]
                } while (operation != null && operationNo < MAX_RECENT_OPERATIONS)
            } else {
                printer.println("    <none>")
            }
        }

        companion object {
            private const val MAX_RECENT_OPERATIONS = 20
            private const val COOKIE_GENERATION_SHIFT = 8
            private const val COOKIE_INDEX_MASK = 0xff
        }
    }

    private class Operation {
        var startTime: Long = 0
        var endTime: Long = 0
        var kind: String? = null
        var sql: String? = null
        var bindArgs: List<Any?>? = null
        var finished: Boolean = false
        var exception: Exception? = null
        var cookie: Int = 0

        private val status: String
            get() = when {
                !finished -> "running"
                exception != null -> "failed"
                else -> "succeeded"
            }

        val formattedStartTime: String
            get() = startTimeDateFormat.format(Date(startTime))

        fun describe(msg: StringBuilder, verbose: Boolean) {
            msg.append(kind)
            if (finished) {
                msg.append(" took ").append(endTime - startTime).append("ms")
            } else {
                msg.append(" started ").append(System.currentTimeMillis() - startTime)
                    .append("ms ago")
            }
            msg.append(" - ").append(status)
            if (sql != null) {
                msg.append(", sql=\"").append(trimSqlForDisplay(sql)).append("\"")
            }
            if (verbose && bindArgs != null && bindArgs!!.size != 0) {
                msg.append(", bindArgs=[")
                val count = bindArgs!!.size
                for (i in 0 until count) {
                    val arg = bindArgs!![i]
                    if (i != 0) {
                        msg.append(", ")
                    }
                    when (arg) {
                        null -> msg.append("null")
                        is ByteArray -> msg.append("<byte[]>")
                        is String -> msg.append("\"").append(arg as String?).append("\"")
                        else -> msg.append(arg)
                    }
                }
                msg.append("]")
            }
            if (exception != null) {
                msg.append(", exception=\"").append(exception!!.message).append("\"")
            }
        }

        companion object {
            @SuppressLint("SimpleDateFormat")
            private val startTimeDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        }
    }

    companion object {
        private const val TAG = "SQLiteConnection"
        private val TRIM_SQL_PATTERN: Pattern = Pattern.compile("[\\s]*\\n+[\\s]*")

        // Called by SQLiteConnectionPool only.
        fun <CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr> open(
            pool: SQLiteConnectionPool<CP, SP>,
            configuration: SqliteDatabaseConfiguration,
            bindings: SqlOpenHelperNativeBindings<CP, SP>,
            connectionId: Int,
            primaryConnection: Boolean,
            debugConfig: SQLiteDebug,
            rootLogger: Logger,
        ): SQLiteConnection<CP, SP> {
            val connection = SQLiteConnection(
                pool = pool,
                configuration = configuration,
                bindings = bindings,
                connectionId = connectionId,
                isPrimaryConnection = primaryConnection,
                debugConfig = debugConfig,
                rootLogger = rootLogger,
            )
            try {
                connection.open()
                return connection
            } catch (ex: SQLiteException) {
                connection.dispose(false)
                throw ex
            }
        }

        private fun canonicalizeSyncMode(value: String?): String = when (value) {
            "0" -> "OFF"
            "1" -> "NORMAL"
            "2" -> "FULL"
            else -> value.toString()
        }

        /**
         * Returns data type of the given object's value.
         *
         *
         * Returned values are
         *
         *  * [Cursor.FIELD_TYPE_NULL]
         *  * [Cursor.FIELD_TYPE_INTEGER]
         *  * [Cursor.FIELD_TYPE_FLOAT]
         *  * [Cursor.FIELD_TYPE_STRING]
         *  * [Cursor.FIELD_TYPE_BLOB]
         *
         *
         *
         * @param obj the object whose value type is to be returned
         * @return object value type
         */
        private fun getTypeOfObject(obj: Any?): Int = when (obj) {
            null -> Cursor.FIELD_TYPE_NULL
            is ByteArray -> Cursor.FIELD_TYPE_BLOB
            is Float, is Double -> Cursor.FIELD_TYPE_FLOAT
            is Long, is Int, is Short, is Byte -> Cursor.FIELD_TYPE_INTEGER
            else -> Cursor.FIELD_TYPE_STRING
        }

        private fun isCacheable(statementType: SQLiteStatementType): Boolean = statementType == STATEMENT_UPDATE ||
                statementType == STATEMENT_SELECT

        private fun trimSqlForDisplay(sql: String?): String {
            return TRIM_SQL_PATTERN.matcher(sql ?: "").replaceAll(" ")
        }
    }
}
