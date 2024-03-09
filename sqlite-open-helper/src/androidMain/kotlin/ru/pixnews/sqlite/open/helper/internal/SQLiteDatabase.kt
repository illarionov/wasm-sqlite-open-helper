/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("FILE_IS_TOO_LONG", "WRONG_OVERLOADING_FUNCTION_ARGUMENTS")

package ru.pixnews.sqlite.open.helper.internal

/*
 * Original Copyrights:
 * Copyright (C) 2017-2024 requery.io
 * Copyright (C) 2005-2012 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteQueryBuilder
import android.database.sqlite.SQLiteTransactionListener
import android.util.Pair
import androidx.core.os.CancellationSignal
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import co.touchlab.kermit.Logger
import ru.pixnews.sqlite.open.helper.OpenFlags
import ru.pixnews.sqlite.open.helper.OpenFlags.Companion.CREATE_IF_NECESSARY
import ru.pixnews.sqlite.open.helper.OpenFlags.Companion.ENABLE_WRITE_AHEAD_LOGGING
import ru.pixnews.sqlite.open.helper.OpenFlags.Companion.OPEN_CREATE
import ru.pixnews.sqlite.open.helper.OpenFlags.Companion.OPEN_READONLY
import ru.pixnews.sqlite.open.helper.SqliteDatabaseConfiguration
import ru.pixnews.sqlite.open.helper.base.CursorWindow
import ru.pixnews.sqlite.open.helper.base.DatabaseErrorHandler
import ru.pixnews.sqlite.open.helper.base.DefaultDatabaseErrorHandler
import ru.pixnews.sqlite.open.helper.common.api.clear
import ru.pixnews.sqlite.open.helper.common.api.contains
import ru.pixnews.sqlite.open.helper.common.api.or
import ru.pixnews.sqlite.open.helper.internal.SQLiteConnectionPool.Companion.CONNECTION_FLAG_PRIMARY_CONNECTION_AFFINITY
import ru.pixnews.sqlite.open.helper.internal.SQLiteConnectionPool.Companion.CONNECTION_FLAG_READ_ONLY
import ru.pixnews.sqlite.open.helper.internal.SQLiteProgram.Companion.bindAllArgsAsStrings
import ru.pixnews.sqlite.open.helper.internal.interop.SqlOpenHelperNativeBindings
import ru.pixnews.sqlite.open.helper.internal.interop.SqlOpenHelperWindowBindings
import ru.pixnews.sqlite.open.helper.internal.interop.Sqlite3ConnectionPtr
import ru.pixnews.sqlite.open.helper.internal.interop.Sqlite3StatementPtr
import ru.pixnews.sqlite.open.helper.internal.interop.Sqlite3WindowPtr
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.util.Locale

/**
 * Exposes methods to manage a SQLite database.
 *
 * SQLiteDatabase has methods to create, delete, execute SQL commands, and
 * perform other common database management tasks.
 *
 * Database names must be unique within an application, not across all applications.
 *
 * ### Localized Collation - ORDER BY
 *
 * In addition to SQLite's default `BINARY` collator, Android supplies
 * two more, `LOCALIZED`, which changes with the system's current locale,
 * and `UNICODE`, which is the Unicode Collation Algorithm and not tailored
 * to the current locale.
 *
 * @param cursorFactory The optional factory to use when creating new Cursors.  May be null.
 */
internal class SQLiteDatabase<
        CP : Sqlite3ConnectionPtr,
        SP : Sqlite3StatementPtr,
        WP : Sqlite3WindowPtr,
        > private constructor(
    configuration: SqliteDatabaseConfiguration,
    private val debugConfig: SQLiteDebug,
    logger: Logger = Logger,
    private val bindings: SqlOpenHelperNativeBindings<CP, SP, WP>,
    private val windowBindings: SqlOpenHelperWindowBindings<WP>,
    private val cursorFactory: CursorFactory<CP, SP, WP>? = null,
    errorHandler: DatabaseErrorHandler? = null,
) : SQLiteClosable(), SupportSQLiteDatabase {
    private val logger = logger.withTag(TAG)

    // Thread-local for database sessions that belong to this database.
    // Each thread has its own database session.
    // INVARIANT: Immutable.
    private val _threadSession: ThreadLocal<SQLiteSession<CP, SP, WP>> =
        object : ThreadLocal<SQLiteSession<CP, SP, WP>>() {
            override fun initialValue(): SQLiteSession<CP, SP, WP> {
                val pool = synchronized(lock) { requireConnectionPoolLocked() }
                return SQLiteSession(pool)
            }
        }

    // Error handler to be used when SQLite returns corruption errors.
    // INVARIANT: Immutable.
    private val errorHandler = errorHandler ?: DefaultDatabaseErrorHandler()

    // Shared database state lock.
    // This lock guards all of the shared state of the database, such as its
    // configuration, whether it is open or closed, and so on.  This lock should
    // be held for as little time as possible.
    //
    // The lock MUST NOT be held while attempting to acquire database connections or
    // while executing SQL statements on behalf of the client as it can lead to deadlock.
    //
    // It is ok to hold the lock while reconfiguring the connection pool or dumping
    // statistics because those operations are non-reentrant and do not try to acquire
    // connections that might be held by other threads.
    //
    // Basic rule: grab the lock, access or modify global state, release the lock, then
    // do the required SQL work.
    private val lock = Any()

    // Warns if the database is finalized without being closed properly.
    // INVARIANT: Guarded by mLock.
    private val closeGuardLocked: CloseGuard = CloseGuard.get()

    // The database configuration.
    // INVARIANT: Guarded by mLock.
    private val configurationLocked = configuration
    private val cursorWindowCtor: (String?) -> CursorWindow<WP> = { name -> CursorWindow(name, windowBindings) }

    // The connection pool for the database, null when closed.
    // The pool itself is thread-safe, but the reference to it can only be acquired
    // when the lock is held.
    // INVARIANT: Guarded by mLock.
    private var connectionPoolLocked: SQLiteConnectionPool<CP, SP, WP>? = null

    /**
     * Gets the [SQLiteSession] that belongs to this thread for this database.
     * Once a thread has obtained a session, it will continue to obtain the same
     * session even after the database has been closed (although the session will not
     * be usable).  However, a thread that does not already have a session cannot
     * obtain one after the database has been closed.
     *
     * The idea is that threads that have active connections to the database may still
     * have work to complete even after the call to [.close].  Active database
     * connections are not actually disposed until they are released by the threads
     * that own them.
     *
     * @return The session, never null.
     * @throws IllegalStateException if the thread does not yet have a session and
     * the database is not open.
     */
    val threadSession: SQLiteSession<CP, SP, WP>
        get() = _threadSession.get()!! // initialValue() throws if database closed

    /**
     * Gets a label to use when describing the database in log messages.
     *
     * @return The label.
     */
    @Suppress("WRONG_NAME_OF_VARIABLE_INSIDE_ACCESSOR")
    val label: String
        get() = synchronized(lock, configurationLocked::label)

    /**
     * Returns true if the current thread is holding an active connection to the database.
     *
     *
     * The name of this method comes from a time when having an active connection
     * to the database meant that the thread was holding an actual lock on the
     * database.  Nowadays, there is no longer a true "database lock" although threads
     * may block if they cannot acquire a database connection to perform a
     * particular operation.
     *
     *
     * @return True if the current thread is holding an active connection to the database.
     */
    override val isDbLockedByCurrentThread: Boolean
        get() = useReference(threadSession::hasConnection)

    @Suppress("NO_CORRESPONDING_PROPERTY", "WRONG_NAME_OF_VARIABLE_INSIDE_ACCESSOR")
    override var version: Int
        /**
         * Gets the database version.
         */
        get() = longForQuery("PRAGMA user_version;").toInt()

        /**
         * Sets the database version.
         */
        set(version) {
            execSQL("PRAGMA user_version = $version")
        }

    override val maximumSize: Long
        /**
         * Returns the maximum size the database may grow to.
         */
        get() {
            val pageCount = longForQuery("PRAGMA max_page_count;")
            return pageCount * pageSize
        }

    override var pageSize: Long
        /**
         * Returns the current database page size, in bytes.
         *
         * @return the database page size, in bytes
         */

        get() = longForQuery("PRAGMA page_size;")

        /**
         * Sets the database page size. The page size must be a power of two. This
         * method does not work if any data has been written to the database file,
         * and must be called right after the database has been created.
         *
         * @param numBytes the database page size, in bytes
         */
        set(numBytes) = execSQL("PRAGMA page_size = $numBytes")

    /**
     * Returns true if the database is opened as read only.
     *
     * @return True if database is opened as read only.
     */
    override val isReadOnly: Boolean
        get() = synchronized(lock) { isReadOnlyLocked }

    private val isReadOnlyLocked: Boolean
        get() = configurationLocked.openFlags.contains(OPEN_READONLY)

    /**
     * Returns true if the database is in-memory db.
     *
     * @return True if the database is in-memory.
     * @hide
     */
    val isInMemoryDatabase: Boolean
        get() = synchronized(lock) { configurationLocked.isInMemoryDb }

    /**
     * Returns true if the database is currently open.
     *
     * @return True if the database is currently open (has not been closed).
     */
    override val isOpen: Boolean
        get() = synchronized(lock) {
            connectionPoolLocked != null
        }

    /**
     * Gets the path to the database file.
     *
     * @return The path to the database file.
     */
    override val path: String?
        get() = synchronized(lock) {
            configurationLocked.path.takeIf { it != SqliteDatabaseConfiguration.MEMORY_DB_PATH }
        }

    /**
     * Returns true if write-ahead logging has been enabled for this database.
     *
     * @see .enableWriteAheadLogging
     * @see .ENABLE_WRITE_AHEAD_LOGGING
     */
    override val isWriteAheadLoggingEnabled: Boolean
        get() {
            synchronized(lock) {
                requireConnectionPoolLocked()
                return configurationLocked.openFlags.contains(ENABLE_WRITE_AHEAD_LOGGING)
            }
        }

    @Suppress("WRONG_NAME_OF_VARIABLE_INSIDE_ACCESSOR")
    override val attachedDbs: List<Pair<String, String>>?
        /**
         * Returns list of full pathnames of all attached databases including the main database
         * by executing 'pragma database_list' on the database.
         *
         * @return ArrayList of pairs of (database name, database file path) or null if the database
         * is not open.
         */
        get() {
            synchronized(lock) {
                if (connectionPoolLocked == null) {
                    return null // not open
                }
                acquireReference()
            }

            try {
                val attachedDbs: MutableList<Pair<String, String>> = mutableListOf()
                // has attached databases. query sqlite to get the list of attached databases.
                rawQuery("pragma database_list;").use { cursor ->
                    while (cursor.moveToNext()) {
                        // sqlite returns a row for each database in the returned list of databases.
                        // in each row,
                        // 1st column is the database name such as main, or the database
                        // name specified on the "ATTACH" command
                        // 2nd column is the database file path.

                        attachedDbs.add(Pair(cursor.getString(1), cursor.getString(2)))
                    }
                }
                return attachedDbs
            } finally {
                releaseReference()
            }
        }

    /**
     * Runs 'pragma integrity_check' on the given database (and all the attached databases)
     * and returns true if the given database (and all its attached databases) pass integrity_check,
     * false otherwise.
     *
     * If the result is false, then this method logs the errors reported by the integrity_check
     * command execution.
     *
     * Note that 'pragma integrity_check' on a database can take a long time.
     *
     * @return true if the given database (and all its attached databases) pass integrity_check,
     * false otherwise.
     */
    override val isDatabaseIntegrityOk: Boolean
        get() = useReference {
            val attachedDbs = try {
                checkNotNull(this.attachedDbs) {
                    "databaselist for: $path couldn't be retrieved. probably because the database is closed"
                }
            } catch (@Suppress("SwallowedException", "TooGenericExceptionCaught") e: SQLiteException) {
                // can't get attachedDb list. do integrity check on the main database
                // TODO: check
                listOf(Pair("main", path))
            }

            attachedDbs.forEach { db ->
                compileStatement("PRAGMA ${db.first}.integrity_check(1);").use { prog ->
                    val rslt = prog.simpleQueryForString()
                    if (!rslt.equals("ok", ignoreCase = true)) {
                        // integrity_checker failed on main or attached databases
                        logger.e { "PRAGMA integrity_check on ${db.second} returned: $rslt" }
                        return false
                    }
                }
            }
            return true
        }

    @Throws(Throwable::class)
    protected fun finalize() = dispose(true)

    override fun onAllReferencesReleased() = dispose(false)

    private fun dispose(finalized: Boolean) {
        val pool: SQLiteConnectionPool<CP, SP, WP>?
        synchronized(lock) {
            if (finalized) {
                closeGuardLocked.warnIfOpen()
            }
            closeGuardLocked.close()
            pool = connectionPoolLocked
            connectionPoolLocked = null
        }

        if (!finalized) {
            pool?.close()
        }
    }

    /**
     * Sends a corruption message to the database error handler.
     */
    fun onCorruption() {
        errorHandler.onCorruption(this)
    }

    /**
     * Gets default connection flags that are appropriate for this thread, taking into
     * account whether the thread is acting on behalf of the UI.
     *
     * @param readOnly True if the connection should be read-only.
     * @return The connection flags.
     */
    fun getThreadDefaultConnectionFlags(readOnly: Boolean): Int {
        val flags = if (readOnly) CONNECTION_FLAG_READ_ONLY else CONNECTION_FLAG_PRIMARY_CONNECTION_AFFINITY
        return flags
    }

    /**
     * Begins a transaction in EXCLUSIVE mode.
     *
     *
     * Transactions can be nested.
     * When the outer transaction is ended all of
     * the work done in that transaction and all of the nested transactions will be committed or
     * rolled back. The changes will be rolled back if any transaction is ended without being
     * marked as clean (by calling setTransactionSuccessful). Otherwise they will be committed.
     *
     *
     * Here is the standard idiom for transactions:
     *
     * <pre>
     * db.beginTransaction();
     * try {
     * ...
     * db.setTransactionSuccessful();
     * } finally {
     * db.endTransaction();
     * }
    </pre> *
     */
    override fun beginTransaction() = beginTransaction(
        null,
        SQLiteSession.TRANSACTION_MODE_EXCLUSIVE,
    )

    /**
     * Begins a transaction in IMMEDIATE mode. Transactions can be nested. When
     * the outer transaction is ended all of the work done in that transaction
     * and all of the nested transactions will be committed or rolled back. The
     * changes will be rolled back if any transaction is ended without being
     * marked as clean (by calling setTransactionSuccessful). Otherwise they
     * will be committed.
     *
     *
     * Here is the standard idiom for transactions:
     *
     * <pre>
     * db.beginTransactionNonExclusive();
     * try {
     * ...
     * db.setTransactionSuccessful();
     * } finally {
     * db.endTransaction();
     * }
    </pre> *
     */
    override fun beginTransactionNonExclusive() = beginTransaction(
        null,
        SQLiteSession.TRANSACTION_MODE_IMMEDIATE,
    )

    /**
     * Begins a transaction in DEFERRED mode.
     */
    fun beginTransactionDeferred() = beginTransaction(
        null,
        SQLiteSession.TRANSACTION_MODE_DEFERRED,
    )

    /**
     * Begins a transaction in DEFERRED mode.
     *
     * @param transactionListener listener that should be notified when the transaction begins,
     * commits, or is rolled back, either explicitly or by a call to
     * [.yieldIfContendedSafely].
     */
    fun beginTransactionWithListenerDeferred(
        transactionListener: SQLiteTransactionListener?,
    ) = beginTransaction(
        transactionListener,
        SQLiteSession.TRANSACTION_MODE_DEFERRED,
    )

    /**
     * Begins a transaction in EXCLUSIVE mode.
     *
     *
     * Transactions can be nested.
     * When the outer transaction is ended all of
     * the work done in that transaction and all of the nested transactions will be committed or
     * rolled back. The changes will be rolled back if any transaction is ended without being
     * marked as clean (by calling setTransactionSuccessful). Otherwise they will be committed.
     *
     *
     * Here is the standard idiom for transactions:
     *
     * <pre>
     * db.beginTransactionWithListener(listener);
     * try {
     * ...
     * db.setTransactionSuccessful();
     * } finally {
     * db.endTransaction();
     * }
    </pre> *
     *
     * @param transactionListener listener that should be notified when the transaction begins,
     * commits, or is rolled back, either explicitly or by a call to
     * [.yieldIfContendedSafely].
     */
    override fun beginTransactionWithListener(
        transactionListener: SQLiteTransactionListener,
    ) = beginTransaction(
        transactionListener,
        SQLiteSession.TRANSACTION_MODE_EXCLUSIVE,
    )

    /**
     * Begins a transaction in IMMEDIATE mode. Transactions can be nested. When
     * the outer transaction is ended all of the work done in that transaction
     * and all of the nested transactions will be committed or rolled back. The
     * changes will be rolled back if any transaction is ended without being
     * marked as clean (by calling setTransactionSuccessful). Otherwise they
     * will be committed.
     *
     *
     * Here is the standard idiom for transactions:
     *
     * <pre>
     * db.beginTransactionWithListenerNonExclusive(listener);
     * try {
     * ...
     * db.setTransactionSuccessful();
     * } finally {
     * db.endTransaction();
     * }
    </pre> *
     *
     * @param transactionListener listener that should be notified when the
     * transaction begins, commits, or is rolled back, either
     * explicitly or by a call to [.yieldIfContendedSafely].
     */
    override fun beginTransactionWithListenerNonExclusive(
        transactionListener: SQLiteTransactionListener,
    ) {
        beginTransaction(
            transactionListener,
            SQLiteSession.TRANSACTION_MODE_IMMEDIATE,
        )
    }

    private fun beginTransaction(transactionListener: SQLiteTransactionListener?, mode: Int) = useReference {
        threadSession.beginTransaction(
            transactionMode = mode,
            transactionListener = transactionListener,
            connectionFlags = getThreadDefaultConnectionFlags(readOnly = false),
            cancellationSignal = null,
        )
    }

    /**
     * End a transaction. See beginTransaction for notes about how to use this and when transactions
     * are committed and rolled back.
     */
    override fun endTransaction() = useReference {
        threadSession.endTransaction(null)
    }

    /**
     * Marks the current transaction as successful. Do not do any more database work between
     * calling this and calling endTransaction. Do as little non-database work as possible in that
     * situation too. If any errors are encountered between this and endTransaction the transaction
     * will still be committed.
     *
     * @throws IllegalStateException if the current thread is not in a transaction or the
     * transaction is already marked as successful.
     */
    override fun setTransactionSuccessful() = useReference(threadSession::setTransactionSuccessful)

    /**
     * Returns true if the current thread has a transaction pending.
     *
     * @return True if the current thread is in a transaction.
     */
    override fun inTransaction(): Boolean = useReference(threadSession::hasTransaction)

    /**
     * Temporarily end the transaction to let other threads run. The transaction is assumed to be
     * successful so far. Do not call setTransactionSuccessful before calling this. When this
     * returns a new transaction will have been created but not marked as successful. This assumes
     * that there are no nested transactions (beginTransaction has only been called once) and will
     * throw an exception if that is not the case.
     *
     * @return true if the transaction was yielded
     */
    override fun yieldIfContendedSafely(): Boolean = yieldIfContendedHelper(
        throwIfUnsafe = true,
        sleepAfterYieldDelay = -1,
    )

    /**
     * Temporarily end the transaction to let other threads run. The transaction is assumed to be
     * successful so far. Do not call setTransactionSuccessful before calling this. When this
     * returns a new transaction will have been created but not marked as successful. This assumes
     * that there are no nested transactions (beginTransaction has only been called once) and will
     * throw an exception if that is not the case.
     *
     * @param sleepAfterYieldDelayMillis if > 0, sleep this long before starting a new transaction if
     * the lock was actually yielded. This will allow other background threads to make some
     * more progress than they would if we started the transaction immediately.
     * @return true if the transaction was yielded
     */
    override fun yieldIfContendedSafely(sleepAfterYieldDelayMillis: Long): Boolean {
        return yieldIfContendedHelper(true, sleepAfterYieldDelay = sleepAfterYieldDelayMillis)
    }

    private fun yieldIfContendedHelper(throwIfUnsafe: Boolean, sleepAfterYieldDelay: Long): Boolean = useReference {
        threadSession.yieldTransaction(sleepAfterYieldDelay, throwIfUnsafe, null)
    }

    /**
     * Reopens the database in read-write mode.
     * If the database is already read-write, does nothing.
     *
     * @throws SQLiteException if the database could not be reopened as requested, in which
     * case it remains open in read only mode.
     * @throws IllegalStateException if the database is not open.
     * @see .isReadOnly
     * @hide
     * @throws RuntimeException
     */
    fun reopenReadWrite(): Unit = synchronized(lock) {
        val pool = requireConnectionPoolLocked()
        if (!isReadOnlyLocked) {
            return // nothing to do
        }

        // Reopen the database in read-write mode.
        val oldOpenFlags = configurationLocked.openFlags
        configurationLocked.openFlags = (configurationLocked.openFlags clear OPEN_READONLY)
        try {
            pool.reconfigure(configurationLocked)
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            configurationLocked.openFlags = oldOpenFlags
            throw ex
        }
    }

    private fun open() = try {
        if (!configurationLocked.isInMemoryDb && configurationLocked.openFlags.contains(OPEN_CREATE)) {
            ensureFile(configurationLocked.path, logger)
        }
        try {
            openInner()
        } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") ex: SQLiteDatabaseCorruptException) {
            onCorruption()
            openInner()
        }
    } catch (ex: SQLiteException) {
        logger.e(ex) { "Failed to open database '$label'." }
        close()
        throw ex
    }

    private fun openInner() = synchronized(lock) {
        check(connectionPoolLocked == null)
        connectionPoolLocked =
            SQLiteConnectionPool.open(configurationLocked, debugConfig, bindings, windowBindings, logger)
        closeGuardLocked.open("close")
    }

    /**
     * Sets the maximum size the database will grow to. The maximum size cannot
     * be set below the current size.
     *
     * @param numBytes the maximum database size, in bytes
     * @return the new maximum database size
     */
    override fun setMaximumSize(numBytes: Long): Long {
        val pageSize = pageSize
        var numPages = numBytes / pageSize
        // If numBytes isn't a multiple of pageSize, bump up a page
        if ((numBytes % pageSize) != 0L) {
            numPages++
        }
        val newPageCount = longForQuery("PRAGMA max_page_count = $numPages")
        return newPageCount * pageSize
    }

    /**
     * Compiles an SQL statement into a reusable pre-compiled statement object.
     * The parameters are identical to [.execSQL]. You may put ?s in the
     * statement and fill in those values with [SQLiteProgram.bindString]
     * and [SQLiteProgram.bindLong] each time you want to run the
     * statement. Statements may not return result sets larger than 1x1.
     *
     *
     * No two threads should be using the same [SQLiteStatement] at the same time.
     *
     * @param sql The raw SQL statement, may contain ? for unknown values to be
     * bound later.
     * @return A pre-compiled [SQLiteStatement] object. Note that
     * [SQLiteStatement]s are not synchronized, see the documentation for more details.
     */
    @Throws(SQLException::class)
    override fun compileStatement(sql: String): SupportSQLiteStatement = useReference {
        SQLiteStatement(this, sql)
    }

    /**
     * Query the given URL, returning a [Cursor] over the result set.
     *
     * @param cursorFactory the cursor factory to use, or null for the default factory
     * @param distinct true if you want each row to be unique, false otherwise.
     * @param table The table name to compile the query against.
     * @param columns A list of which columns to return. Passing null will
     * return all columns, which is discouraged to prevent reading
     * data from storage that isn't going to be used.
     * @param selection A filter declaring which rows to return, formatted as an
     * SQL WHERE clause (excluding the WHERE itself). Passing null
     * will return all rows for the given table.
     * @param selectionArgs You may include ?s in selection, which will be
     * replaced by the values from selectionArgs, in order that they
     * appear in the selection.
     * @param groupBy A filter declaring how to group rows, formatted as an SQL
     * GROUP BY clause (excluding the GROUP BY itself). Passing null
     * will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in the cursor,
     * if row grouping is being used, formatted as an SQL HAVING
     * clause (excluding the HAVING itself). Passing null will cause
     * all row groups to be included, and is required when row
     * grouping is not being used.
     * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
     * (excluding the ORDER BY itself). Passing null will use the
     * default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query,
     * formatted as LIMIT clause. Passing null denotes no LIMIT clause.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * If the operation is canceled, then [OperationCanceledException] will be thrown
     * when the query is executed.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     * @see Cursor
     */

    /**
     * Query the given URL, returning a [Cursor] over the result set.
     *
     * @param cursorFactory the cursor factory to use, or null for the default factory
     * @param distinct true if you want each row to be unique, false otherwise.
     * @param table The table name to compile the query against.
     * @param columns A list of which columns to return. Passing null will
     * return all columns, which is discouraged to prevent reading
     * data from storage that isn't going to be used.
     * @param selection A filter declaring which rows to return, formatted as an
     * SQL WHERE clause (excluding the WHERE itself). Passing null
     * will return all rows for the given table.
     * @param selectionArgs You may include ?s in selection, which will be
     * replaced by the values from selectionArgs, in order that they
     * appear in the selection.
     * @param groupBy A filter declaring how to group rows, formatted as an SQL
     * GROUP BY clause (excluding the GROUP BY itself). Passing null
     * will cause the rows to not be grouped.
     * @param having A filter declare which row groups to include in the cursor,
     * if row grouping is being used, formatted as an SQL HAVING
     * clause (excluding the HAVING itself). Passing null will cause
     * all row groups to be included, and is required when row
     * grouping is not being used.
     * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
     * (excluding the ORDER BY itself). Passing null will use the
     * default sort order, which may be unordered.
     * @param limit Limits the number of rows returned by the query,
     * formatted as LIMIT clause. Passing null denotes no LIMIT clause.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     * @see Cursor
     */
    @JvmOverloads
    @Suppress("LongParameterList")
    internal fun queryWithFactory(
        cursorFactory: CursorFactory<CP, SP, WP>?,
        distinct: Boolean,
        table: String,
        columns: Array<String?>?,
        selection: String?,
        selectionArgs: List<Any?>,
        groupBy: String?,
        having: String?,
        orderBy: String?,
        limit: String?,
        cancellationSignal: CancellationSignal? = null,
    ): Cursor = useReference {
        val sql = SQLiteQueryBuilder.buildQueryString(
            distinct,
            table,
            columns,
            selection,
            groupBy,
            having,
            orderBy,
            limit,
        )
        rawQueryWithFactory(cursorFactory, sql, selectionArgs, cancellationSignal)
    }

    /**
     * Runs the provided SQL and returns a [Cursor] over the result set.
     *
     * @param query the SQL query. The SQL string must not be ; terminated
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     */
    override fun query(
        query: String,
    ): Cursor = rawQueryWithFactory(
        cursorFactory = null,
        sql = query,
        cancellationSignal = null,
    )

    /**
     * Runs the provided SQL and returns a [Cursor] over the result set.
     *
     * @param query the SQL query. The SQL string must not be ; terminated
     * @param selectionArgs You may include ?s in where clause in the query,
     * which will be replaced by the values from selectionArgs.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     */
    override fun query(
        query: String,
        bindArgs: Array<out Any?>,
    ): Cursor = rawQueryWithFactory(
        cursorFactory = null,
        sql = query,
        selectionArgs = bindArgs.toList(),
        cancellationSignal = null,
    )

    /**
     * Runs the provided SQL and returns a [Cursor] over the result set.
     *
     * @param query the SQL query.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     */
    override fun query(query: SupportSQLiteQuery): Cursor = query(query, signal = null)

    /**
     * Runs the provided SQL and returns a [Cursor] over the result set.
     *
     * @param query the SQL query. The SQL string must not be ; terminated
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * If the operation is canceled, then [OperationCanceledException] will be thrown
     * when the query is executed.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     */
    override fun query(
        query: SupportSQLiteQuery,
        cancellationSignal: android.os.CancellationSignal?,
    ): Cursor {
        if (cancellationSignal != null) {
            val supportCancellationSignal = CancellationSignal()
            cancellationSignal.setOnCancelListener(supportCancellationSignal::cancel)
            return query(query, supportCancellationSignal)
        } else {
            return query(query, signal = null)
        }
    }

    /**
     * Runs the provided SQL and returns a [Cursor] over the result set.
     *
     * @param supportQuery the SQL query. The SQL string must not be ; terminated
     * @param signal A signal to cancel the operation in progress, or null if none.
     * If the operation is canceled, then [OperationCanceledException] will be thrown
     * when the query is executed.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     */
    fun query(
        supportQuery: SupportSQLiteQuery,
        signal: CancellationSignal?,
    ): Cursor = rawQueryWithFactory(
        cursorFactory = { db, masterQuery, query ->
            supportQuery.bindTo(query)
            cursorFactory?.newCursor(db, masterQuery, query) ?: SQLiteCursor(
                checkNotNull(masterQuery),
                query,
                cursorWindowCtor,
                logger,
            )
        },
        sql = supportQuery.sql,
        selectionArgs = listOf(),
        cancellationSignal = signal,
    )

    /**
     * Runs the provided SQL and returns a cursor over the result set.
     *
     * @param cursorFactory the cursor factory to use, or null for the default factory
     * @param sql the SQL query. The SQL string must not be ; terminated
     * @param selectionArgs You may include ?s in where clause in the query,
     * which will be replaced by the values from selectionArgs.
     * @param editTable the name of the first table, which is editable
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * If the operation is canceled, then [OperationCanceledException] will be thrown
     * when the query is executed.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     */

    /**
     * Runs the provided SQL and returns a cursor over the result set.
     *
     * @param cursorFactory the cursor factory to use, or null for the default factory
     * @param sql the SQL query. The SQL string must not be ; terminated
     * @param selectionArgs You may include ?s in where clause in the query,
     * which will be replaced by the values from selectionArgs.
     * @param editTable the name of the first table, which is editable
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     */
    @JvmOverloads
    internal fun rawQueryWithFactory(
        cursorFactory: CursorFactory<CP, SP, WP>?,
        sql: String,
        selectionArgs: List<Any?> = listOf(),
        cancellationSignal: CancellationSignal? = null,
    ): Cursor = useReference {
        val driver: SQLiteCursorDriver<CP, SP, WP> = SQLiteDirectCursorDriver(
            database = this,
            sql = sql,
            cancellationSignal = cancellationSignal,
            cursorWindowCtor = cursorWindowCtor,
            logger = logger,
        )
        return driver.query(cursorFactory ?: this.cursorFactory, selectionArgs)
    }

    /**
     * Runs the provided SQL and returns a [Cursor] over the result set.
     *
     * @param sql the SQL query. The SQL string must not be ; terminated
     * @param selectionArgs You may include ?s in where clause in the query,
     * which will be replaced by the values from selectionArgs.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * If the operation is canceled, then [OperationCanceledException] will be thrown
     * when the query is executed.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     */
    fun rawQuery(
        sql: String,
        selectionArgs: List<Any> = listOf(),
        cancellationSignal: CancellationSignal? = null,
    ): Cursor = rawQueryWithFactory(null, sql, selectionArgs, cancellationSignal)

    /**
     * General method for inserting a row into the database.
     *
     * @param table the table to insert the row into
     * @param conflictAlgorithm for insert conflict resolver
     * @param values this map contains the initial column values for the
     * row. The keys should be the column names and the values the
     * column values
     * @return the row ID of the newly inserted row
     * OR the primary key of the existing row if the input param 'conflictAlgorithm' =
     * [.CONFLICT_IGNORE]
     * OR -1 if any error
     */
    @Throws(SQLException::class)
    override fun insert(
        table: String,
        conflictAlgorithm: Int,
        values: ContentValues,
    ): Long {
        return insertWithOnConflict(table, null, values, ConflictAlgorithm.entitiesMap.getValue(conflictAlgorithm))
    }

    /**
     * General method for inserting a row into the database.
     *
     * @param table the table to insert the row into
     * @param nullColumnHack optional; may be `null`.
     * SQL doesn't allow inserting a completely empty row without
     * naming at least one column name.  If your provided `initialValues` is
     * empty, no column names are known and an empty row can't be inserted.
     * If not set to null, the `nullColumnHack` parameter
     * provides the name of nullable column name to explicitly insert a NULL into
     * in the case where your `initialValues` is empty.
     * @param initialValues this map contains the initial column values for the
     * row. The keys should be the column names and the values the
     * column values
     * @param conflictAlgorithm for insert conflict resolver
     * @return the row ID of the newly inserted row
     * OR the primary key of the existing row if the input param 'conflictAlgorithm' =
     * [.CONFLICT_IGNORE]
     * OR -1 if any error
     */
    fun insertWithOnConflict(
        table: String?,
        nullColumnHack: String?,
        initialValues: ContentValues?,
        conflictAlgorithm: ConflictAlgorithm,
    ): Long = useReference {
        // TODO: too verbose
        val sql = StringBuilder()
        sql.append("INSERT")
        sql.append(conflictAlgorithm.sql)
        sql.append(" INTO ")
        sql.append(table)
        sql.append('(')

        val bindArgs: Array<Any?>
        val size = if ((initialValues != null && initialValues.size() > 0)) {
            initialValues.size()
        } else {
            0
        }
        if (size > 0) {
            bindArgs = arrayOfNulls(size)
            var i = 0
            for ((key, value) in initialValues!!.valueSet()) {
                sql.append(if ((i > 0)) "," else "")
                sql.append(key)
                bindArgs[i++] = value
            }
            sql.append(')')
            sql.append(" VALUES (")
            i = 0
            while (i < size) {
                sql.append(if ((i > 0)) ",?" else "?")
                i++
            }
        } else {
            sql.append("$nullColumnHack) VALUES (NULL")
            bindArgs = emptyArray()
        }
        sql.append(')')

        return SQLiteStatement(
            this,
            sql.toString(),
            bindArgs.toList(),
        ).use { it.executeInsert() }
    }

    /**
     * Convenience method for deleting rows in the database.
     *
     * @param table the table to delete from
     * @param whereClause the optional WHERE clause to apply when deleting.
     * Passing null will delete all rows.
     * @param whereArgs You may include ?s in the where clause, which
     * will be replaced by the values from whereArgs. The values
     * will be bound as Strings.
     * @return the number of rows affected if a whereClause is passed in, 0
     * otherwise. To remove all rows and get a count pass "1" as the
     * whereClause.
     */
    fun delete(table: String, whereClause: String, whereArgs: List<String?> = emptyList()): Int = useReference {
        SQLiteStatement(
            this,
            "DELETE FROM $table${(if (whereClause.isNotEmpty()) " WHERE $whereClause" else "")}",
            whereArgs,
        ).use {
            it.executeUpdateDelete()
        }
    }

    /**
     * Convenience method for deleting rows in the database.
     *
     * @param table the table to delete from
     * @param whereClause the optional WHERE clause to apply when deleting.
     * Passing null will delete all rows.
     * @param whereArgs You may include ?s in the where clause, which
     * will be replaced by the values from whereArgs. The values
     * will be bound as Strings.
     * @return the number of rows affected if a whereClause is passed in, 0
     * otherwise. To remove all rows and get a count pass "1" as the
     * whereClause.
     */
    override fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?): Int = useReference {
        SQLiteStatement(
            this,
            "DELETE FROM $table${(if (whereClause?.isNotEmpty() == true) " WHERE $whereClause" else "")}",
            whereArgs?.toList() ?: emptyList(),
        ).use {
            it.executeUpdateDelete()
        }
    }

    /**
     * Convenience method for updating rows in the database.
     *
     * @param table the table to update in
     * @param values a map from column names to new column values. null is a
     * valid value that will be translated to NULL.
     * @param whereClause the optional WHERE clause to apply when updating.
     * Passing null will update all rows.
     * @param whereArgs You may include ?s in the where clause, which
     * will be replaced by the values from whereArgs. The values
     * will be bound as Strings.
     * @return the number of rows affected
     */
    internal fun update(table: String?, values: ContentValues, whereClause: String?, whereArgs: List<String?>): Int {
        return update(table, values, whereClause, whereArgs, ConflictAlgorithm.CONFLICT_NONE)
    }

    /**
     * Convenience method for updating rows in the database.
     *
     * @param table the table to update in
     * @param values a map from column names to new column values. null is a
     * valid value that will be translated to NULL.
     * @param whereClause the optional WHERE clause to apply when updating.
     * Passing null will update all rows.
     * @param whereArgs You may include ?s in the where clause, which
     * will be replaced by the values from whereArgs. The values
     * will be bound as Strings.
     * @param conflictAlgorithm for update conflict resolver
     * @return the number of rows affected
     */
    override fun update(
        table: String,
        conflictAlgorithm: Int,
        values: ContentValues,
        whereClause: String?,
        whereArgs: Array<out Any?>?,
    ): Int = update(
        table,
        values,
        whereClause,
        whereArgs?.asList() ?: emptyList(),
        ConflictAlgorithm.entitiesMap.getValue(conflictAlgorithm),
    )

    private fun update(
        table: String?,
        values: ContentValues,
        whereClause: String?,
        whereArgs: List<Any?>,
        conflictAlgorithm: ConflictAlgorithm,
    ): Int = useReference {
        require(values.size() != 0) { "Empty values" }

        val sql = buildString {
            append("UPDATE ")
            append(conflictAlgorithm.sql)
            append(table)
            append(" SET ")

            values.keySet().joinTo(this) { "$it=?" }

            whereClause?.let {
                if (it.isNotEmpty()) {
                    append(" WHERE ")
                    append(it)
                }
            }
        }

        val bindArgs = values.valueSet().map(Map.Entry<*, *>::value) + whereArgs
        return SQLiteStatement(this, sql, bindArgs).use(SQLiteStatement<*>::executeUpdateDelete)
    }

    /**
     * Execute a single SQL statement that is NOT a SELECT
     * or any other SQL statement that returns data.
     *
     *
     * It has no means to return any data (such as the number of affected rows).
     * Instead, you're encouraged to use [.insert],
     * [.update], et al, when possible.
     *
     *
     *
     * When using [.enableWriteAheadLogging], journal_mode is
     * automatically managed by this class. So, do not set journal_mode
     * using "PRAGMA journal_mode'<value>" statement if your app is using
     * [.enableWriteAheadLogging]
    </value> *
     *
     * @param sql the SQL statement to be executed. Multiple statements separated by semicolons are
     * not supported.
     * @throws SQLException if the SQL string is invalid
     */
    @Throws(SQLException::class)
    override fun execSQL(sql: String) {
        executeSql(sql)
    }

    /**
     * Execute a single SQL statement that is NOT a SELECT/INSERT/UPDATE/DELETE.
     *
     *
     * For INSERT statements, use any of the following instead.
     *
     *  * [.insert]
     *  * [.insertOrThrow]
     *  * [.insertWithOnConflict]
     *
     *
     *
     * For UPDATE statements, use any of the following instead.
     *
     *  * [.update]
     *  * [.updateWithOnConflict]
     *
     *
     *
     * For DELETE statements, use any of the following instead.
     *
     *  * [.delete]
     *
     *
     *
     * For example, the following are good candidates for using this method:
     *
     *  * ALTER TABLE
     *  * CREATE or DROP table / trigger / view / index / virtual table
     *  * REINDEX
     *  * RELEASE
     *  * SAVEPOINT
     *  * PRAGMA that returns no data
     *
     * When using [.enableWriteAheadLogging], journal_mode is
     * automatically managed by this class. So, do not set journal_mode
     * using "PRAGMA journal_mode'<value>" statement if your app is using
     * [.enableWriteAheadLogging]
    </value> *
     *
     * @param sql the SQL statement to be executed. Multiple statements separated by semicolons are
     * not supported.
     * @param bindArgs only byte[], String, Long and Double are supported in bindArgs.
     * @throws SQLException if the SQL string is invalid
     */
    @Throws(SQLException::class)
    override fun execSQL(sql: String, bindArgs: Array<out Any?>) {
        executeSql(sql, bindArgs.toList())
    }

    @Throws(SQLException::class)
    private fun executeSql(sql: String, bindArgs: List<Any?> = emptyList()): Int = useReference {
        SQLiteStatement(this, sql, bindArgs)
            .use(SQLiteStatement<*>::executeUpdateDelete)
    }

    /**
     * Verifies that a SQL SELECT statement is valid by compiling it.
     * If the SQL statement is not valid, this method will throw a [SQLiteException].
     *
     * @param sql SQL to be validated
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * If the operation is canceled, then [OperationCanceledException] will be thrown
     * when the query is executed.
     * @throws SQLiteException if `sql` is invalid
     */
    fun validateSql(sql: String, cancellationSignal: CancellationSignal?) {
        threadSession.prepare(sql, getThreadDefaultConnectionFlags(true), cancellationSignal)
    }

    /**
     * Returns true if the new version code is greater than the current database version.
     *
     * @param newVersion The new version code.
     * @return True if the new version code is greater than the current database version.
     */
    override fun needUpgrade(newVersion: Int): Boolean = newVersion > version

    /**
     * Sets the locale for this database.
     *
     * @param locale The new locale.
     * @throws SQLException if the locale could not be set.  The most common reason
     * for this is that there is no collator available for the locale you requested.
     * In this case the database remains unchanged.
     */
    override fun setLocale(locale: Locale) = synchronized(lock) {
        val pool = requireConnectionPoolLocked()
        val oldLocale = configurationLocked.locale
        configurationLocked.locale = locale
        try {
            pool.reconfigure(configurationLocked)
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            configurationLocked.locale = oldLocale
            throw ex
        }
    }

    /**
     * Sets the maximum size of the prepared-statement cache for this database.
     * (size of the cache = number of compiled-sql-statements stored in the cache).
     *
     *
     * Maximum cache size can ONLY be increased from its current size (default = 10).
     * If this method is called with smaller size than the current maximum value,
     * then IllegalStateException is thrown.
     *
     *
     * This method is thread-safe.
     *
     * @param cacheSize the size of the cache. can be (0 to [.MAX_SQL_CACHE_SIZE])
     * @throws IllegalStateException if input cacheSize > [.MAX_SQL_CACHE_SIZE].
     */
    override fun setMaxSqlCacheSize(cacheSize: Int) {
        check(!(cacheSize > MAX_SQL_CACHE_SIZE || cacheSize < 0)) { "expected value between 0 and $MAX_SQL_CACHE_SIZE" }

        synchronized(lock) {
            val pool = requireConnectionPoolLocked()
            val oldMaxSqlCacheSize = configurationLocked.maxSqlCacheSize
            configurationLocked.maxSqlCacheSize = cacheSize
            try {
                pool.reconfigure(configurationLocked)
            } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
                configurationLocked.maxSqlCacheSize = oldMaxSqlCacheSize
                throw ex
            }
        }
    }

    /**
     * Sets whether foreign key constraints are enabled for the database.
     *
     *
     * By default, foreign key constraints are not enforced by the database.
     * This method allows an application to enable foreign key constraints.
     * It must be called each time the database is opened to ensure that foreign
     * key constraints are enabled for the session.
     *
     *
     * A good time to call this method is right after calling [.openOrCreateDatabase]
     * or in the [RequerySqliteOpenHelper.onConfigure] callback.
     *
     *
     * When foreign key constraints are disabled, the database does not check whether
     * changes to the database will violate foreign key constraints.  Likewise, when
     * foreign key constraints are disabled, the database will not execute cascade
     * delete or update triggers.  As a result, it is possible for the database
     * state to become inconsistent.  To perform a database integrity check,
     * call [.isDatabaseIntegrityOk].
     *
     *
     * This method must not be called while a transaction is in progress.
     *
     *
     * See also [SQLite Foreign Key Constraints](http://sqlite.org/foreignkeys.html)
     * for more details about foreign key constraint support.
     *
     *
     * @param enabled True to enable foreign key constraints, false to disable them.
     * @throws IllegalStateException if the are transactions is in progress
     * when this method is called.
     */
    override fun setForeignKeyConstraintsEnabled(enabled: Boolean) = synchronized(lock) {
        val pool = requireConnectionPoolLocked()
        if (configurationLocked.foreignKeyConstraintsEnabled == enabled) {
            return
        }

        configurationLocked.foreignKeyConstraintsEnabled = enabled
        try {
            pool.reconfigure(configurationLocked)
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            configurationLocked.foreignKeyConstraintsEnabled = !enabled
            throw ex
        }
    }

    /**
     * This method enables parallel execution of queries from multiple threads on the
     * same database.  It does this by opening multiple connections to the database
     * and using a different database connection for each query.  The database
     * journal mode is also changed to enable writes to proceed concurrently with reads.
     *
     *
     * When write-ahead logging is not enabled (the default), it is not possible for
     * reads and writes to occur on the database at the same time.  Before modifying the
     * database, the writer implicitly acquires an exclusive lock on the database which
     * prevents readers from accessing the database until the write is completed.
     *
     *
     * In contrast, when write-ahead logging is enabled (by calling this method), write
     * operations occur in a separate log file which allows reads to proceed concurrently.
     * While a write is in progress, readers on other threads will perceive the state
     * of the database as it was before the write began.  When the write completes, readers
     * on other threads will then perceive the new state of the database.
     *
     *
     * It is a good idea to enable write-ahead logging whenever a database will be
     * concurrently accessed and modified by multiple threads at the same time.
     * However, write-ahead logging uses significantly more memory than ordinary
     * journaling because there are multiple connections to the same database.
     * So if a database will only be used by a single thread, or if optimizing
     * concurrency is not very important, then write-ahead logging should be disabled.
     *
     *
     * After calling this method, execution of queries in parallel is enabled as long as
     * the database remains open.  To disable execution of queries in parallel, either
     * call [.disableWriteAheadLogging] or close the database and reopen it.
     *
     *
     * The maximum number of connections used to execute queries in parallel is
     * dependent upon the device memory and possibly other properties.
     *
     *
     * If a query is part of a transaction, then it is executed on the same database handle the
     * transaction was begun.
     *
     *
     * Writers should use [.beginTransactionNonExclusive] or
     * [.beginTransactionWithListenerNonExclusive]
     * to start a transaction.  Non-exclusive mode allows database file to be in readable
     * by other threads executing queries.
     *
     *
     * If the database has any attached databases, then execution of queries in parallel is NOT
     * possible.  Likewise, write-ahead logging is not supported for read-only databases
     * or memory databases.  In such cases, [.enableWriteAheadLogging] returns false.
     *
     *
     * The best way to enable write-ahead logging is to pass the
     * [.ENABLE_WRITE_AHEAD_LOGGING] flag to [.openDatabase].  This is
     * more efficient than calling [.enableWriteAheadLogging].
     * `<pre>
     * SQLiteDatabase db = SQLiteDatabase.openDatabase("db_filename", cursorFactory,
     * SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING,
     * myDatabaseErrorHandler);
     * db.enableWriteAheadLogging();
    </pre>` *
     *
     *
     * Another way to enable write-ahead logging is to call [.enableWriteAheadLogging]
     * after opening the database.
     * `<pre>
     * SQLiteDatabase db = SQLiteDatabase.openDatabase("db_filename", cursorFactory,
     * SQLiteDatabase.CREATE_IF_NECESSARY, myDatabaseErrorHandler);
     * db.enableWriteAheadLogging();
    </pre>` *
     *
     *
     * See also [SQLite Write-Ahead Logging](http://sqlite.org/wal.html) for
     * more details about how write-ahead logging works.
     *
     * @return True if write-ahead logging is enabled.
     * @throws IllegalStateException if there are transactions in progress at the
     * time this method is called.  WAL mode can only be changed when there are no
     * transactions in progress.
     *
     * @see .ENABLE_WRITE_AHEAD_LOGGING
     * @see .disableWriteAheadLogging
     */
    override fun enableWriteAheadLogging(): Boolean = synchronized(lock) {
        val pool = requireConnectionPoolLocked()
        if (configurationLocked.openFlags.contains(ENABLE_WRITE_AHEAD_LOGGING)) {
            return true
        }

        if (isReadOnlyLocked) {
            // WAL doesn't make sense for readonly-databases.
            // TODO: True, but connection pooling does still make sense...
            return false
        }

        if (configurationLocked.isInMemoryDb) {
            logger.i { "can't enable WAL for memory databases." }
            return false
        }

        configurationLocked.openFlags = configurationLocked.openFlags or ENABLE_WRITE_AHEAD_LOGGING
        try {
            pool.reconfigure(configurationLocked)
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            configurationLocked.openFlags = configurationLocked.openFlags.clear(ENABLE_WRITE_AHEAD_LOGGING)
            throw ex
        }
        return true
    }

    /**
     * This method disables the features enabled by [.enableWriteAheadLogging].
     *
     * @throws IllegalStateException if there are transactions in progress at the
     * time this method is called.  WAL mode can only be changed when there are no
     * transactions in progress.
     *
     * @see .enableWriteAheadLogging
     */
    override fun disableWriteAheadLogging() = synchronized(lock) {
        val pool = requireConnectionPoolLocked()
        if (configurationLocked.openFlags.contains(ENABLE_WRITE_AHEAD_LOGGING)) {
            return
        }

        configurationLocked.openFlags = configurationLocked.openFlags clear ENABLE_WRITE_AHEAD_LOGGING
        try {
            pool.reconfigure(configurationLocked)
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            configurationLocked.openFlags = configurationLocked.openFlags or ENABLE_WRITE_AHEAD_LOGGING
            throw ex
        }
    }

    private fun collectDbStats(dbStatsList: ArrayList<DbStats>) {
        synchronized(lock) {
            connectionPoolLocked?.collectDbStats(dbStatsList)
        }
    }

    override fun toString(): String = "SQLiteDatabase: $path"

    private fun requireConnectionPoolLocked(): SQLiteConnectionPool<CP, SP, WP> = checkNotNull(connectionPoolLocked) {
        "The database '${configurationLocked.label}' is not open."
    }

    /**
     * Query the table for the number of rows in the table.
     *
     * @param table the name of the table to query
     * @param selection A filter declaring which rows to return,
     * formatted as an SQL WHERE clause (excluding the WHERE itself).
     * Passing null will count all rows for the given table
     * @param selectionArgs You may include ?s in selection,
     * which will be replaced by the values from selectionArgs,
     * in order that they appear in the selection.
     * The values will be bound as Strings.
     * @return the number of rows in the table filtered by the selection
     */
    /**
     * Query the table for the number of rows in the table.
     *
     * @param table the name of the table to query
     * @return the number of rows in the table
     */

    /**
     * Query the table for the number of rows in the table.
     *
     * @param table the name of the table to query
     * @param selection A filter declaring which rows to return,
     * formatted as an SQL WHERE clause (excluding the WHERE itself).
     * Passing null will count all rows for the given table
     * @return the number of rows in the table filtered by the selection
     */
    fun queryNumEntries(table: String, selection: String? = null, selectionArgs: List<String> = listOf()): Long {
        val whereSelection = if (selection?.isNotEmpty() == true) " where $selection" else ""
        return longForQuery("select count(*) from $table$whereSelection", selectionArgs)
    }

    /**
     * Utility method to run the query on the db and return the value in the
     * first column of the first row.
     */
    fun longForQuery(
        query: String,
        selectionArgs: List<String?> = emptyList(),
    ): Long = compileStatement(query).use { prog ->
        longForQuery(prog = prog, selectionArgs)
    }

    /**
     * Utility method to run the query on the db and return the value in the
     * first column of the first row.
     */
    fun stringForQuery(
        query: String,
        selectionArgs: List<String> = emptyList(),
    ): String? = compileStatement(query).use { prog -> stringForQuery(prog, selectionArgs) }

    /**
     * Used to allow returning sub-classes of [Cursor] when calling query.
     */
    internal fun interface CursorFactory<CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr, WP : Sqlite3WindowPtr> {
        /**
         * See [SQLiteCursor.SQLiteCursor].
         */
        fun newCursor(
            db: SQLiteDatabase<CP, SP, WP>,
            masterQuery: SQLiteCursorDriver<CP, SP, WP>?,
            query: SQLiteQuery<WP>,
        ): Cursor
    }

    companion object {
        private const val TAG = "SQLiteDatabase"

        /**
         * Absolute max value that can be set by [.setMaxSqlCacheSize].
         *
         * Each prepared-statement is between 1K - 6K, depending on the complexity of the
         * SQL statement & schema.  A large SQL cache may use a significant amount of memory.
         */
        const val MAX_SQL_CACHE_SIZE: Int = 100

        /**
         * Attempts to release memory that SQLite holds but does not require to
         * operate properly. Typically this memory will come from the page cache.
         *
         * @return the number of bytes actually released
         */
        fun releaseMemory(): Int {
            return SQLiteGlobal.releaseMemory()
        }

        /**
         * Open the database according to the flags [OpenFlags]
         *
         *
         * Sets the locale of the database to the  the system's current locale.
         * Call [.setLocale] if you would like something else.
         *
         *
         * Accepts input param: a concrete instance of [DatabaseErrorHandler] to be
         * used to handle corruption when sqlite reports database corruption.
         *
         * @param path to database file to open and/or create
         * @param factory an optional factory class that is called to instantiate a
         * cursor when query is called, or null for default
         * @param flags to control database access mode
         * @param errorHandler the [DatabaseErrorHandler] obj to be used to handle corruption
         * when sqlite reports database corruption
         * @return the newly opened database
         * @throws SQLiteException if the database cannot be opened
         */

        /**
         * Open the database according to the flags [OpenFlags]
         *
         *
         * Sets the locale of the database to the  the system's current locale.
         * Call [.setLocale] if you would like something else.
         *
         * @param path to database file to open and/or create
         * @param factory an optional factory class that is called to instantiate a
         * cursor when query is called, or null for default
         * @param flags to control database access mode
         * @return the newly opened database
         * @throws SQLiteException if the database cannot be opened
         */
        @JvmStatic
        internal fun <CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr, WP : Sqlite3WindowPtr> openDatabase(
            path: String,
            factory: CursorFactory<CP, SP, WP>?,
            flags: OpenFlags,
            errorHandler: DatabaseErrorHandler? = null,
            bindings: SqlOpenHelperNativeBindings<CP, SP, WP>,
            windowBindings: SqlOpenHelperWindowBindings<WP>,
            debugConfig: SQLiteDebug,
            logger: Logger = Logger,
        ): SQLiteDatabase<CP, SP, WP> {
            val configuration = SqliteDatabaseConfiguration(path, flags)
            val db = SQLiteDatabase(configuration, debugConfig, logger, bindings, windowBindings, factory, errorHandler)
            db.open()
            return db
        }

        /**
         * Open the database according to the given configuration.
         *
         *
         * Sets the locale of the database to the  the system's current locale.
         * Call [.setLocale] if you would like something else.
         *
         *
         * Accepts input param: a concrete instance of [DatabaseErrorHandler] to be
         * used to handle corruption when sqlite reports database corruption.
         *
         * @param configuration to database configuration to use
         * @param factory an optional factory class that is called to instantiate a
         * cursor when query is called, or null for default
         * @param errorHandler the [DatabaseErrorHandler] obj to be used to handle corruption
         * when sqlite reports database corruption
         * @return the newly opened database
         * @throws SQLiteException if the database cannot be opened
         */
        internal fun <CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr, WP : Sqlite3WindowPtr> openDatabase(
            configuration: SqliteDatabaseConfiguration,
            factory: CursorFactory<CP, SP, WP>?,
            errorHandler: DatabaseErrorHandler?,
            bindings: SqlOpenHelperNativeBindings<CP, SP, WP>,
            windowBindings: SqlOpenHelperWindowBindings<WP>,
            debugConfig: SQLiteDebug,
            logger: Logger = Logger,
        ): SQLiteDatabase<CP, SP, WP> {
            val db = SQLiteDatabase(configuration, debugConfig, logger, bindings, windowBindings, factory, errorHandler)
            db.open()
            return db
        }

        /**
         * Equivalent to openDatabase(file.getPath(), factory, CREATE_IF_NECESSARY).
         */
        internal fun <CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr, WP : Sqlite3WindowPtr> openOrCreateDatabase(
            file: File,
            factory: CursorFactory<CP, SP, WP>?,
            bindings: SqlOpenHelperNativeBindings<CP, SP, WP>,
            windowBindings: SqlOpenHelperWindowBindings<WP>,
            debugConfig: SQLiteDebug,
        ): SQLiteDatabase<CP, SP, WP> = openOrCreateDatabase(file.path, factory, bindings, windowBindings, debugConfig)

        /**
         * Equivalent to openDatabase(path, factory, CREATE_IF_NECESSARY).
         */
        internal fun <CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr, WP : Sqlite3WindowPtr> openOrCreateDatabase(
            path: String,
            factory: CursorFactory<CP, SP, WP>?,
            bindings: SqlOpenHelperNativeBindings<CP, SP, WP>,
            windowBindings: SqlOpenHelperWindowBindings<WP>,
            debugConfig: SQLiteDebug,
        ): SQLiteDatabase<CP, SP, WP> = openDatabase(
            path = path,
            factory = factory,
            flags = CREATE_IF_NECESSARY,
            errorHandler = null,
            bindings = bindings,
            windowBindings = windowBindings,
            debugConfig = debugConfig,
        )

        /**
         * Equivalent to openDatabase(path, factory, CREATE_IF_NECESSARY, errorHandler).
         */
        @JvmStatic
        internal fun <CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr, WP : Sqlite3WindowPtr> openOrCreateDatabase(
            path: String,
            factory: CursorFactory<CP, SP, WP>,
            errorHandler: DatabaseErrorHandler?,
            bindings: SqlOpenHelperNativeBindings<CP, SP, WP>,
            windowBindings: SqlOpenHelperWindowBindings<WP>,
            debugConfig: SQLiteDebug,
        ): SQLiteDatabase<CP, SP, WP> =
            openDatabase(path, factory, CREATE_IF_NECESSARY, errorHandler, bindings, windowBindings, debugConfig)

        /**
         * Deletes a database including its journal file and other auxiliary files
         * that may have been created by the database engine.
         *
         * @param file The database file path.
         * @return True if the database was successfully deleted.
         */
        @JvmStatic
        fun deleteDatabase(file: File?): Boolean {
            requireNotNull(file) { "file must not be null" }

            var deleted: Boolean
            deleted = file.delete()
            deleted = deleted or File(file.path + "-journal").delete()
            deleted = deleted or File(file.path + "-shm").delete()
            deleted = deleted or File(file.path + "-wal").delete()

            val dir = file.parentFile
            if (dir != null) {
                val prefix = file.name + "-mj"
                val filter = FileFilter { candidate -> candidate.name.startsWith(prefix) }
                dir.listFiles(filter)?.forEach { masterJournal ->
                    deleted = deleted or masterJournal.delete()
                }
            }
            return deleted
        }

        @Suppress("NestedBlockDepth")
        private fun ensureFile(path: String, logger: Logger = Logger) {
            val file = File(path)
            if (!file.exists()) {
                try {
                    val created = file.parentFile?.let { dir ->
                        if (!dir.exists()) {
                            dir.mkdirs()
                        } else {
                            true
                        }
                    } ?: false
                    if (!created) {
                        // Fixes #103: Check parent directory's existence before
                        // attempting to create.
                        logger.e { "Couldn't mkdirs $file" }
                    }
                    if (!file.createNewFile()) {
                        logger.e { "Couldn't create $file" }
                    }
                } catch (e: IOException) {
                    logger.e(e) { "Couldn't ensure file $file" }
                }
            }
        }

        /**
         * Create a memory backed SQLite database.  Its contents will be destroyed
         * when the database is closed.
         *
         * Sets the locale of the database to the the system's current locale.
         * Call [.setLocale] if you would like something else.
         *
         * @param factory an optional factory class that is called to instantiate a
         * cursor when query is called
         * @return a SQLiteDatabase object, or null if the database can't be created
         */
        internal fun <CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr, WP : Sqlite3WindowPtr> create(
            factory: CursorFactory<CP, SP, WP>?,
            bindings: SqlOpenHelperNativeBindings<CP, SP, WP>,
            windowBindings: SqlOpenHelperWindowBindings<WP>,
            debugConfig: SQLiteDebug,
        ): SQLiteDatabase<CP, SP, WP> = openDatabase(
            path = SqliteDatabaseConfiguration.MEMORY_DB_PATH,
            factory = factory,
            flags = CREATE_IF_NECESSARY,
            bindings = bindings,
            windowBindings = windowBindings,
            debugConfig = debugConfig,
        )

        /**
         * Finds the name of the first table, which is editable.
         *
         * @param tables a list of tables
         * @return the first table listed
         */
        @JvmStatic
        fun findEditTable(tables: String): String {
            if (tables.isNotEmpty()) {
                // find the first word terminated by either a space or a comma
                val spacepos = tables.indexOf(' ')
                val commapos = tables.indexOf(',')

                if (spacepos > 0 && (spacepos < commapos || commapos < 0)) {
                    return tables.substring(0, spacepos)
                } else if (commapos > 0 && (commapos < spacepos || spacepos < 0)) {
                    return tables.substring(0, commapos)
                }
                return tables
            } else {
                error("Invalid tables")
            }
        }

        /**
         * Utility method to run the pre-compiled query and return the value in the
         * first column of the first row.
         */
        private fun longForQuery(prog: SupportSQLiteStatement, selectionArgs: List<String?>): Long {
            prog.bindAllArgsAsStrings(selectionArgs)
            return prog.simpleQueryForLong()
        }

        /**
         * Utility method to run the pre-compiled query and return the value in the
         * first column of the first row.
         */
        fun stringForQuery(prog: SupportSQLiteStatement, selectionArgs: List<String?>): String? {
            prog.bindAllArgsAsStrings(selectionArgs)
            return prog.simpleQueryForString()
        }

        /**
         * Query the given table, returning a [Cursor] over the result set.
         *
         * @param table The table name to compile the query against.
         * @param columns A list of which columns to return. Passing null will
         * return all columns, which is discouraged to prevent reading
         * data from storage that isn't going to be used.
         * @param selection A filter declaring which rows to return, formatted as an
         * SQL WHERE clause (excluding the WHERE itself). Passing null
         * will return all rows for the given table.
         * @param selectionArgs You may include ?s in selection, which will be
         * replaced by the values from selectionArgs, in order that they
         * appear in the selection.
         * @param groupBy A filter declaring how to group rows, formatted as an SQL
         * GROUP BY clause (excluding the GROUP BY itself). Passing null
         * will cause the rows to not be grouped.
         * @param having A filter declare which row groups to include in the cursor,
         * if row grouping is being used, formatted as an SQL HAVING
         * clause (excluding the HAVING itself). Passing null will cause
         * all row groups to be included, and is required when row
         * grouping is not being used.
         * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
         * (excluding the ORDER BY itself). Passing null will use the
         * default sort order, which may be unordered.
         * @param limit Limits the number of rows returned by the query,
         * formatted as LIMIT clause. Passing null denotes no LIMIT clause.
         * @return A [Cursor] object, which is positioned before the first entry. Note that
         * [Cursor]s are not synchronized, see the documentation for more details.
         * @see Cursor
         */
        fun SQLiteDatabase<Sqlite3ConnectionPtr, Sqlite3StatementPtr, Sqlite3WindowPtr>.query(
            table: String,
            columns: Array<String?>?,
            selection: String?,
            selectionArgs: List<Any?>,
            groupBy: String?,
            having: String?,
            orderBy: String?,
            limit: String? = null,
        ): Cursor = query(
            distinct = false,
            table = table,
            columns = columns,
            selection = selection,
            selectionArgs = selectionArgs,
            groupBy = groupBy,
            having = having,
            orderBy = orderBy,
            limit = limit,
        )

        /**
         * Query the given URL, returning a [Cursor] over the result set.
         *
         * @param distinct true if you want each row to be unique, false otherwise.
         * @param table The table name to compile the query against.
         * @param columns A list of which columns to return. Passing null will
         * return all columns, which is discouraged to prevent reading
         * data from storage that isn't going to be used.
         * @param selection A filter declaring which rows to return, formatted as an
         * SQL WHERE clause (excluding the WHERE itself). Passing null
         * will return all rows for the given table.
         * @param selectionArgs You may include ?s in selection, which will be
         * replaced by the values from selectionArgs, in order that they
         * appear in the selection.
         * @param groupBy A filter declaring how to group rows, formatted as an SQL
         * GROUP BY clause (excluding the GROUP BY itself). Passing null
         * will cause the rows to not be grouped.
         * @param having A filter declare which row groups to include in the cursor,
         * if row grouping is being used, formatted as an SQL HAVING
         * clause (excluding the HAVING itself). Passing null will cause
         * all row groups to be included, and is required when row
         * grouping is not being used.
         * @param orderBy How to order the rows, formatted as an SQL ORDER BY clause
         * (excluding the ORDER BY itself). Passing null will use the
         * default sort order, which may be unordered.
         * @param limit Limits the number of rows returned by the query,
         * formatted as LIMIT clause. Passing null denotes no LIMIT clause.
         * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
         * If the operation is canceled, then [OperationCanceledException] will be thrown
         * when the query is executed.
         * @return A [Cursor] object, which is positioned before the first entry. Note that
         * [Cursor]s are not synchronized, see the documentation for more details.
         * @see Cursor
         */
        fun SQLiteDatabase<Sqlite3ConnectionPtr, Sqlite3StatementPtr, Sqlite3WindowPtr>.query(
            distinct: Boolean,
            table: String,
            columns: Array<String?>?,
            selection: String?,
            selectionArgs: List<Any?>,
            groupBy: String?,
            having: String?,
            orderBy: String?,
            limit: String?,
            cancellationSignal: CancellationSignal? = null,
        ): Cursor = queryWithFactory(
            cursorFactory = null,
            distinct = distinct,
            table = table,
            columns = columns,
            selection = selection,
            selectionArgs = selectionArgs,
            groupBy = groupBy,
            having = having,
            orderBy = orderBy,
            limit = limit,
            cancellationSignal = cancellationSignal,
        )

        /**
         * Convenience method for inserting a row into the database.
         *
         * @param table the table to insert the row into
         * @param nullColumnHack optional; may be `null`.
         * SQL doesn't allow inserting a completely empty row without
         * naming at least one column name.  If your provided `values` is
         * empty, no column names are known and an empty row can't be inserted.
         * If not set to null, the `nullColumnHack` parameter
         * provides the name of nullable column name to explicitly insert a NULL into
         * in the case where your `values` is empty.
         * @param values this map contains the initial column values for the
         * row. The keys should be the column names and the values the
         * column values
         * @return the row ID of the newly inserted row, or -1 if an error occurred
         */
        fun SQLiteDatabase<*, *, *>.insert(table: String?, nullColumnHack: String?, values: ContentValues): Long = try {
            insertWithOnConflict(table, nullColumnHack, values, ConflictAlgorithm.CONFLICT_NONE)
        } catch (e: SQLException) {
            logger.e(e) { "Error inserting $values" }
            -1
        }

        /**
         * Convenience method for inserting a row into the database.
         *
         * @param table the table to insert the row into
         * @param nullColumnHack optional; may be `null`.
         * SQL doesn't allow inserting a completely empty row without
         * naming at least one column name.  If your provided `values` is
         * empty, no column names are known and an empty row can't be inserted.
         * If not set to null, the `nullColumnHack` parameter
         * provides the name of nullable column name to explicitly insert a NULL into
         * in the case where your `values` is empty.
         * @param values this map contains the initial column values for the
         * row. The keys should be the column names and the values the
         * column values
         * @return the row ID of the newly inserted row, or -1 if an error occurred
         * @throws SQLException
         */
        @Throws(SQLException::class)
        fun SQLiteDatabase<*, *, *>.insertOrThrow(
            table: String?,
            nullColumnHack: String?,
            values: ContentValues?,
        ): Long {
            return insertWithOnConflict(table, nullColumnHack, values, ConflictAlgorithm.CONFLICT_NONE)
        }

        /**
         * Convenience method for replacing a row in the database.
         *
         * @param table the table in which to replace the row
         * @param nullColumnHack optional; may be `null`.
         * SQL doesn't allow inserting a completely empty row without
         * naming at least one column name.  If your provided `initialValues` is
         * empty, no column names are known and an empty row can't be inserted.
         * If not set to null, the `nullColumnHack` parameter
         * provides the name of nullable column name to explicitly insert a NULL into
         * in the case where your `initialValues` is empty.
         * @param initialValues this map contains the initial column values for
         * the row.
         * @return the row ID of the newly inserted row, or -1 if an error occurred
         */
        fun SQLiteDatabase<*, *, *>.replace(
            table: String?,
            nullColumnHack: String?,
            initialValues: ContentValues,
        ): Long {
            try {
                return insertWithOnConflict(table, nullColumnHack, initialValues, ConflictAlgorithm.CONFLICT_REPLACE)
            } catch (e: SQLException) {
                logger.e(e) { "Error inserting $initialValues" }
                return -1
            }
        }

        /**
         * Convenience method for replacing a row in the database.
         *
         * @param table the table in which to replace the row
         * @param nullColumnHack optional; may be `null`.
         * SQL doesn't allow inserting a completely empty row without
         * naming at least one column name.  If your provided `initialValues` is
         * empty, no column names are known and an empty row can't be inserted.
         * If not set to null, the `nullColumnHack` parameter
         * provides the name of nullable column name to explicitly insert a NULL into
         * in the case where your `initialValues` is empty.
         * @param initialValues this map contains the initial column values for
         * the row. The key
         * @return the row ID of the newly inserted row, or -1 if an error occurred
         * @throws SQLException
         */
        @Throws(SQLException::class)
        fun SQLiteDatabase<*, *, *>.replaceOrThrow(
            table: String?,
            nullColumnHack: String?,
            initialValues: ContentValues?,
        ): Long = insertWithOnConflict(table, nullColumnHack, initialValues, ConflictAlgorithm.CONFLICT_REPLACE)
    }
}

public interface SQLiteDatabaseFunction {
    /**
     * Invoked whenever the function is called.
     *
     * @param args function arguments
     * @return String value of the result or null
     */
    public fun callback(args: Args?, result: Result?)
    public interface Args {
        public fun getBlob(arg: Int): ByteArray?
        public fun getString(arg: Int): String?
        public fun getDouble(arg: Int): Double
        public fun getInt(arg: Int): Int
        public fun getLong(arg: Int): Long
    }

    public interface Result {
        public fun set(value: ByteArray?)
        public fun set(value: Double)
        public fun set(value: Int)
        public fun set(value: Long)
        public fun set(value: String?)
        public fun setError(error: String?)
        public fun setNull()
    }

    public companion object {
        /**
         * Flag that declares this function to be "deterministic,"
         * which means it may be used with Indexes on Expressions.
         */
        public const val FLAG_DETERMINISTIC: Int = 0x800
    }
}
