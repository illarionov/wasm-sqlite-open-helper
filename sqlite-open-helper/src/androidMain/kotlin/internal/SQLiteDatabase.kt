/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("FILE_IS_TOO_LONG", "WRONG_OVERLOADING_FUNCTION_ARGUMENTS")

package ru.pixnews.wasm.sqlite.open.helper.internal

/*
 * Original Copyrights:
 * Copyright (C) 2017-2024 requery.io
 * Copyright (C) 2005-2012 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

import android.content.ContentValues
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteTransactionListener
import android.util.Pair
import androidx.annotation.GuardedBy
import androidx.core.os.CancellationSignal
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.SupportSQLiteStatement
import at.released.weh.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.Locale
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion.CREATE_IF_NECESSARY
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion.ENABLE_WRITE_AHEAD_LOGGING
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion.OPEN_CREATE
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion.OPEN_READONLY
import ru.pixnews.wasm.sqlite.open.helper.clear
import ru.pixnews.wasm.sqlite.open.helper.contains
import ru.pixnews.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfig
import ru.pixnews.wasm.sqlite.open.helper.exception.AndroidSqliteDatabaseCorruptException
import ru.pixnews.wasm.sqlite.open.helper.internal.CloseGuard.CloseGuardFinalizeAction
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteConnectionPool.Companion.CONNECTION_FLAG_PRIMARY_CONNECTION_AFFINITY
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteConnectionPool.Companion.CONNECTION_FLAG_READ_ONLY
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteDatabaseConfiguration.Companion.isInMemoryDb
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteDatabaseConfiguration.Companion.resolveJournalMode
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteProgram.Companion.bindAllArgsAsStrings
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteSession.Companion.TRANSACTION_MODE_EXCLUSIVE
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteSession.Companion.TRANSACTION_MODE_IMMEDIATE
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteStatementType.Companion.getSqlStatementType
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteStatementType.STATEMENT_DDL
import ru.pixnews.wasm.sqlite.open.helper.internal.ext.DatabaseUtils
import ru.pixnews.wasm.sqlite.open.helper.or
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDatabaseJournalMode
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDatabaseJournalMode.WAL
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDatabaseSyncMode
import java.io.File
import java.io.IOException
import kotlin.collections.Map.Entry

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
 */
internal class SQLiteDatabase private constructor(
    private val debugConfig: WasmSqliteDebugConfig,
    rootLogger: Logger,
    private val bindings: OpenHelperNativeBindings,
    path: String,
    openFlags: OpenFlags,
    defaultLocale: Locale = Locale.EN_US,
    private val errorHandler: DatabaseErrorHandler,
    lookasideSlotSize: Int,
    lookasideSlotCount: Int,
    journalMode: SqliteDatabaseJournalMode?,
    syncMode: SqliteDatabaseSyncMode?,
) : SQLiteClosable(), SupportSQLiteDatabase {
    private val logger = rootLogger.withTag("SQLiteDatabase")

    // Thread-local for database sessions that belong to this database.
    // Each thread has its own database session.
    // INVARIANT: Immutable.
    private val _threadSession: ThreadLocal<SQLiteSession> = ThreadLocal.withInitial {
        val pool = synchronized(lock) { requireConnectionPoolLocked() }
        SQLiteSession(pool)
    }

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
    @GuardedBy("lock")
    private val closeGuardLocked: CloseGuard = CloseGuard.get()
    private val closeGuardCleaner = wasmSqliteCleaner.register(this, CloseGuardFinalizeAction(closeGuardLocked))

    // The database configuration.
    @GuardedBy("lock")
    private val configurationLocked = SQLiteDatabaseConfiguration(
        path = path,
        openFlags = openFlags,
        locale = defaultLocale,
        lookasideSlotSize = lookasideSlotSize,
        lookasideSlotCount = lookasideSlotCount,
        journalMode = journalMode,
        syncMode = syncMode,
    )

    // The connection pool for the database, null when closed.
    // The pool itself is thread-safe, but the reference to it can only be acquired
    // when the lock is held.
    @GuardedBy("lock")
    private var connectionPoolLocked: SQLiteConnectionPool? = null

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
    val threadSession: SQLiteSession
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

    /**
     * Database version
     */
    @Suppress("NO_CORRESPONDING_PROPERTY", "WRONG_NAME_OF_VARIABLE_INSIDE_ACCESSOR")
    override var version: Int
        get() = longForQuery("PRAGMA user_version;").toInt()
        set(version) {
            execSQL("PRAGMA user_version = $version")
        }

    /**
     * Returns the maximum size the database may grow to.
     */
    override val maximumSize: Long
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
            configurationLocked.path.takeIf { it != SQLiteDatabaseConfiguration.MEMORY_DB_PATH }
        }

    /**
     * Returns true if write-ahead logging has been enabled for this database.
     *
     * @see .enableWriteAheadLogging
     * @see .ENABLE_WRITE_AHEAD_LOGGING
     */
    @Suppress("NO_CORRESPONDING_PROPERTY")
    override val isWriteAheadLoggingEnabled: Boolean
        get() {
            synchronized(lock) {
                requireConnectionPoolLocked()
                return configurationLocked.resolveJournalMode() == WAL
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
                rawQueryWithFactory("pragma database_list;").use { cursor ->
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

    override fun onAllReferencesReleased() {
        val pool: SQLiteConnectionPool?
        synchronized(lock) {
            closeGuardLocked.close()
            closeGuardCleaner.clean()
            pool = connectionPoolLocked
            connectionPoolLocked = null
        }
        pool?.close()
    }

    /**
     * Sends a corruption message to the database error handler.
     */
    fun onCorruption() {
        errorHandler.onCorruption(this)
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
    override fun beginTransaction() = beginTransaction(null, TRANSACTION_MODE_EXCLUSIVE)

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
    override fun beginTransactionNonExclusive() = beginTransaction(null, TRANSACTION_MODE_IMMEDIATE)

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
    ) = beginTransaction(transactionListener, TRANSACTION_MODE_EXCLUSIVE)

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
            TRANSACTION_MODE_IMMEDIATE,
        )
    }

    /**
     * Begin a transaction with the specified mode.  Valid modes are
     * [SQLiteSession.TRANSACTION_MODE_DEFERRED], [TRANSACTION_MODE_IMMEDIATE], and [TRANSACTION_MODE_EXCLUSIVE].
     */
    private fun beginTransaction(transactionListener: SQLiteTransactionListener?, mode: Int) = useReference {
        // DEFERRED transactions are read-only to allows concurrent read-only transactions.
        // Others are read/write.
        val readOnly = (mode == SQLiteSession.TRANSACTION_MODE_DEFERRED)
        threadSession.beginTransaction(
            transactionMode = mode,
            transactionListener = transactionListener,
            connectionFlags = getThreadDefaultConnectionFlags(readOnly),
            cancellationSignal = null,
        )
    }

    /**
     * End a transaction. See beginTransaction for notes about how to use this and when transactions
     * are committed and rolled back.
     */
    override fun endTransaction() = useReference { threadSession.endTransaction() }

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
    override fun yieldIfContendedSafely(): Boolean = yieldIfContendedSafely(-1)

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
    override fun yieldIfContendedSafely(sleepAfterYieldDelayMillis: Long): Boolean = useReference {
        threadSession.yieldTransaction(sleepAfterYieldDelayMillis, true, null)
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
        } catch (ex: AndroidSqliteDatabaseCorruptException) {
            if (isCorruptException(ex)) {
                onCorruption()
                openInner()
            } else {
                throw ex
            }
        }
    } catch (ex: SQLiteException) {
        logger.e(ex) { "Failed to open database '$label'." }
        close()
        throw ex
    }

    private fun openInner() = synchronized(lock) {
        check(connectionPoolLocked == null)
        connectionPoolLocked = SQLiteConnectionPool.open(configurationLocked, debugConfig, bindings, logger)
        closeGuardLocked.open("close")
    }

    /**
     * Execute the given SQL statement on all connections to this database.
     *
     * This statement will be immediately executed on all existing connections,
     * and will be automatically executed on all future connections.
     *
     * Some example usages are changes like `PRAGMA trusted_schema=OFF` or
     * functions like `SELECT icu_load_collation()`. If you execute these
     * statements using [.execSQL] then they will only apply to a single
     * database connection; using this method will ensure that they are
     * uniformly applied to all current and future connections.
     *
     * @param sql The SQL statement to be executed. Multiple statements
     * separated by semicolons are not supported.
     * @param bindArgs The arguments that should be bound to the SQL statement.
     * @throws SQLException
     * @throws RuntimeException
     */
    @Throws(SQLException::class)
    fun execPerConnectionSQL(sql: String, bindArgs: List<Any?> = emptyList()) {
        // Copy arguments to ensure that the caller doesn't accidentally change
        // the values used by future connections
        val bindArgsCopy = DatabaseUtils.deepCopyOf(bindArgs)

        synchronized(lock) {
            val pool = requireConnectionPoolLocked()
            val index: Int = configurationLocked.perConnectionSql.size
            configurationLocked.perConnectionSql.add(sql to bindArgsCopy)
            try {
                pool.reconfigure(configurationLocked)
            } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
                configurationLocked.perConnectionSql.removeAt(index)
                throw ex
            }
        }
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
     * replaced by the values from selectionArgs, in the order that they
     * appear in the selection. The values will be bound as Strings.
     * If selection is null or does not contain ?s then selectionArgs
     * may be null.
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
     * Runs the provided SQL and returns a [Cursor] over the result set.
     *
     * @param query the SQL query. The SQL string must not be ; terminated
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     */
    override fun query(query: String): Cursor = rawQueryWithFactory(query)

    /**
     * Runs the provided SQL and returns a [Cursor] over the result set.
     *
     * @param query the SQL query. The SQL string must not be ; terminated
     * @param selectionArgs You may include ?s in where clause in the query,
     * which will be replaced by the values from selectionArgs.
     * @return A [Cursor] object, which is positioned before the first entry. Note that
     * [Cursor]s are not synchronized, see the documentation for more details.
     */
    override fun query(query: String, bindArgs: Array<out Any?>): Cursor = rawQueryWithFactory(
        sql = query,
        selectionArgs = bindArgs.toList(),
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
        sql = supportQuery.sql,
        cancellationSignal = signal,
        cursorFactory = { query ->
            supportQuery.bindTo(query)
            SQLiteCursor(query, logger)
        },
    )

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
     * @throws RuntimeException
     */
    @JvmOverloads
    internal fun rawQueryWithFactory(
        sql: String,
        selectionArgs: List<Any?> = listOf(),
        cancellationSignal: CancellationSignal? = null,
        cursorFactory: ((SQLiteQuery) -> Cursor)? = null,
    ): Cursor = useReference {
        val query = SQLiteQuery(this, sql, selectionArgs, cancellationSignal, logger)
        try {
            return cursorFactory?.invoke(query) ?: SQLiteCursor(query, logger)
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            query.close()
            throw ex
        }
    }

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
    ): Long = insertWithOnConflict(
        table = table,
        nullColumnHack = null,
        initialValues = values,
        conflictAlgorithm = ConflictAlgorithm.entitiesMap.getValue(conflictAlgorithm),
    )

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
            sql.append(nullColumnHack).append(") VALUES (NULL")
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
    override fun delete(table: String, whereClause: String?, whereArgs: Array<out Any?>?): Int = useReference {
        SQLiteStatement(
            this,
            "DELETE FROM $table${(if (!whereClause.isNullOrEmpty()) " WHERE $whereClause" else "")}",
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
     * @param conflictAlgorithm for update conflict resolver
     * @return the number of rows affected
     */
    override fun update(
        table: String,
        conflictAlgorithm: Int,
        values: ContentValues,
        whereClause: String?,
        whereArgs: Array<out Any?>?,
    ): Int = useReference {
        require(values.size() != 0) { "Empty values" }

        val sql = buildString {
            append("UPDATE ")
            append(ConflictAlgorithm.entitiesMap.getValue(conflictAlgorithm).sql)
            append(table)
            append(" SET ")

            values.keySet().joinTo(this) { "$it=?" }

            if (!whereClause.isNullOrEmpty()) {
                append(" WHERE ")
                append(whereClause)
            }
        }

        val bindArgs = values.valueSet().map(Entry<*, *>::value) + (whereArgs?.asList() ?: emptyList())
        return SQLiteStatement(this, sql, bindArgs).use(SQLiteStatement::executeUpdateDelete)
    }

    /**
     * Execute a single SQL statement that is NOT a SELECT or any other SQL statement that returns data.
     *
     * It has no means to return any data (such as the number of affected rows).
     * Instead, you're encouraged to use [insert], [update], et al, when possible.
     *
     * When using [enableWriteAheadLogging], journal_mode is automatically managed by this class.
     * So, do not set journal_mode using "PRAGMA journal_mode <value>" statement if your app is using
     * [enableWriteAheadLogging]
     *
     * Note that `PRAGMA` values which apply on a per-connection basis should _not_ be configured using this method;
     * you should instead use [execPerConnectionSQL] to ensure that they are uniformly applied to all current
     * and future connections.
     *
     * @param sql the SQL statement to be executed. Multiple statements separated by semicolons are not supported.
     * @throws SQLException if the SQL string is invalid
     */
    @Throws(SQLException::class)
    override fun execSQL(sql: String) {
        executeSql(sql)
    }

    /**
     * Execute a single SQL statement that is NOT a SELECT/INSERT/UPDATE/DELETE.
     *
     * For INSERT statements, use any of the following instead.
     *
     *  * [insert]
     *  * [insertOrThrow]
     *  * [insertWithOnConflict]
     *
     * For UPDATE statements, use any of the following instead.
     *
     *  * [update]
     *  * [updateWithOnConflict]
     *
     * For DELETE statements, use any of the following instead.
     *
     *  * [delete]
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
     * When using [enableWriteAheadLogging], journal_mode is
     * automatically managed by this class. So, do not set journal_mode
     * using "PRAGMA journal_mode <value>" statement if your app is using [enableWriteAheadLogging]
     *
     * Note that `PRAGMA` values which apply on a per-connection basis should _not_ be configured using this method;
     * you should instead use [execPerConnectionSQL] to ensure that they are uniformly applied to all current
     * and future connections.
     *
     * @param sql the SQL statement to be executed. Multiple statements separated by semicolons are not supported.
     * @param bindArgs only byte[], String, Long and Double are supported in bindArgs.
     * @throws SQLException if the SQL string is invalid
     */
    @Throws(SQLException::class)
    override fun execSQL(sql: String, bindArgs: Array<out Any?>) {
        executeSql(sql, bindArgs.toList())
    }

    @Throws(SQLException::class)
    private fun executeSql(sql: String, bindArgs: List<Any?> = emptyList()): Int = useReference {
        val statementType = getSqlStatementType(sql)
        try {
            SQLiteStatement(this, sql, bindArgs).use {
                it.executeUpdateDelete()
            }
        } finally {
            // If schema was updated, close non-primary connections and clear prepared
            // statement caches of active connections, otherwise they might have outdated
            // schema information.
            if (statementType == STATEMENT_DDL) {
                requireConnectionPoolLocked().run {
                    closeAvailableNonPrimaryConnectionsAndLogExceptions()
                    clearAcquiredConnectionsPreparedStatementCache()
                }
            }
        }
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
    override fun setLocale(locale: java.util.Locale) = synchronized(lock) {
        val pool = requireConnectionPoolLocked()
        val oldLocale = configurationLocked.locale
        configurationLocked.locale = Locale(locale.toString())
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
     * By default, foreign key constraints are not enforced by the database.
     * This method allows an application to enable foreign key constraints.
     * It must be called each time the database is opened to ensure that foreign
     * key constraints are enabled for the session.
     *
     * A good time to call this method is right after calling [.openOrCreateDatabase]
     * or in the [RequerySqliteOpenHelper.onConfigure] callback.
     *
     * When foreign key constraints are disabled, the database does not check whether
     * changes to the database will violate foreign key constraints.  Likewise, when
     * foreign key constraints are disabled, the database will not execute cascade
     * delete or update triggers.  As a result, it is possible for the database
     * state to become inconsistent.  To perform a database integrity check,
     * call [.isDatabaseIntegrityOk].
     *
     * This method must not be called while a transaction is in progress.
     *
     * See also [SQLite Foreign Key Constraints](http://sqlite.org/foreignkeys.html)
     * for more details about foreign key constraint support.
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
     * When write-ahead logging is not enabled (the default), it is not possible for
     * reads and writes to occur on the database at the same time.  Before modifying the
     * database, the writer implicitly acquires an exclusive lock on the database which
     * prevents readers from accessing the database until the write is completed.
     *
     * In contrast, when write-ahead logging is enabled (by calling this method), write
     * operations occur in a separate log file which allows reads to proceed concurrently.
     * While a write is in progress, readers on other threads will perceive the state
     * of the database as it was before the write began.  When the write completes, readers
     * on other threads will then perceive the new state of the database.
     *
     * It is a good idea to enable write-ahead logging whenever a database will be
     * concurrently accessed and modified by multiple threads at the same time.
     * However, write-ahead logging uses significantly more memory than ordinary
     * journaling because there are multiple connections to the same database.
     * So if a database will only be used by a single thread, or if optimizing
     * concurrency is not very important, then write-ahead logging should be disabled.
     *
     * After calling this method, execution of queries in parallel is enabled as long as
     * the database remains open.  To disable execution of queries in parallel, either
     * call [.disableWriteAheadLogging] or close the database and reopen it.
     *
     * The maximum number of connections used to execute queries in parallel is
     * dependent upon the device memory and possibly other properties.
     *
     * If a query is part of a transaction, then it is executed on the same database handle the
     * transaction was begun.
     *
     * Writers should use [.beginTransactionNonExclusive] or
     * [.beginTransactionWithListenerNonExclusive]
     * to start a transaction.  Non-exclusive mode allows database file to be in readable
     * by other threads executing queries.
     *
     * If the database has any attached databases, then execution of queries in parallel is NOT
     * possible.  Likewise, write-ahead logging is not supported for read-only databases
     * or memory databases.  In such cases, [.enableWriteAheadLogging] returns false.
     *
     * The best way to enable write-ahead logging is to pass the
     * [.ENABLE_WRITE_AHEAD_LOGGING] flag to [.openDatabase].  This is
     * more efficient than calling [.enableWriteAheadLogging].
     * `<pre>
     * SQLiteDatabase db = SQLiteDatabase.openDatabase("db_filename", cursorFactory,
     * SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING,
     * myDatabaseErrorHandler);
     *
     * Another way to enable write-ahead logging is to call [.enableWriteAheadLogging]
     * after opening the database.
     * `<pre>
     * SQLiteDatabase db = SQLiteDatabase.openDatabase("db_filename", cursorFactory,
     * SQLiteDatabase.CREATE_IF_NECESSARY, myDatabaseErrorHandler);
     * db.enableWriteAheadLogging();
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
        if (configurationLocked.resolveJournalMode() == SqliteDatabaseJournalMode.WAL) {
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
    override fun disableWriteAheadLogging(): Unit = synchronized(lock) {
        if (configurationLocked.resolveJournalMode() != SqliteDatabaseJournalMode.WAL) {
            return
        }

        val pool = requireConnectionPoolLocked()

        val oldFlags = configurationLocked.openFlags
        // If an app explicitly disables WAL, it takes priority over any directive
        // to use the legacy "compatibility WAL" mode.
        configurationLocked.openFlags = configurationLocked.openFlags clear (ENABLE_WRITE_AHEAD_LOGGING)
        try {
            pool.reconfigure(configurationLocked)
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            configurationLocked.openFlags = oldFlags
            throw ex
        }
    }

    private fun requireConnectionPoolLocked(): SQLiteConnectionPool = checkNotNull(connectionPoolLocked) {
        "The database '${configurationLocked.label}' is not open."
    }

    override fun toString(): String = "SQLiteDatabase: $path"

    /**
     * Utility method to run the query on the db and return the value in the
     * first column of the first row.
     */
    private fun longForQuery(
        query: String,
        selectionArgs: List<String?> = emptyList(),
    ): Long = compileStatement(query).use { statement: SupportSQLiteStatement ->
        statement.bindAllArgsAsStrings(selectionArgs)
        statement.simpleQueryForLong()
    }

    internal fun getStatementCacheMissRate(): Double = requireConnectionPoolLocked().getStatementCacheMissRate()

    internal fun getTotalPreparedStatements(): Int = requireConnectionPoolLocked().totalPrepareStatements

    internal fun getTotalStatementCacheMisses(): Int = requireConnectionPoolLocked().totalPrepareStatementCacheMiss

    internal companion object {
        /**
         * Absolute max value that can be set by [.setMaxSqlCacheSize].
         *
         * Each prepared-statement is between 1K - 6K, depending on the complexity of the
         * SQL statement & schema.  A large SQL cache may use a significant amount of memory.
         */
        const val MAX_SQL_CACHE_SIZE: Int = 100

        /**
         * Open the database according to the given configuration.
         *
         * Accepts input param: a concrete instance of [DatabaseErrorHandler] to be
         * used to handle corruption when sqlite reports database corruption.
         *
         * @param factory an optional factory class that is called to instantiate a
         * cursor when query is called, or null for default
         * @param errorHandler the [DatabaseErrorHandler] obj to be used to handle corruption
         * when sqlite reports database corruption
         * @return the newly opened database
         * @throws SQLiteException if the database cannot be opened
         */
        internal fun openDatabase(
            path: String,
            defaultLocale: Locale,
            openParams: SQLiteDatabaseOpenParams,
            bindings: OpenHelperNativeBindings,
            debugConfig: WasmSqliteDebugConfig,
            logger: Logger,
        ): SQLiteDatabase {
            val db = SQLiteDatabase(
                debugConfig = debugConfig,
                rootLogger = logger,
                bindings = bindings,
                path = path,
                openFlags = CREATE_IF_NECESSARY,
                defaultLocale = defaultLocale,
                errorHandler = openParams.errorHandler ?: error("Error handler not set"),
                journalMode = openParams.journalMode,
                lookasideSlotSize = openParams.lookasideSlotSize,
                lookasideSlotCount = openParams.lookasideSlotCount,
                syncMode = openParams.synchronousMode,
            )
            db.open()
            return db
        }

        @Suppress("NestedBlockDepth")
        private fun ensureFile(path: String, logger: Logger) {
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
         * @return a SQLiteDatabase instance
         * @throws AndroidSqliteException if the database cannot be created
         */
        internal fun createInMemory(
            bindings: OpenHelperNativeBindings,
            openParams: SQLiteDatabaseOpenParams,
            debugConfig: WasmSqliteDebugConfig,
            logger: Logger,
        ): SQLiteDatabase {
            val db = SQLiteDatabase(
                debugConfig = debugConfig,
                rootLogger = logger,
                bindings = bindings,
                path = SQLiteDatabaseConfiguration.MEMORY_DB_PATH,
                openFlags = openParams.openFlags or CREATE_IF_NECESSARY,
                defaultLocale = openParams.locale,
                errorHandler = openParams.errorHandler ?: error("Error handler not set"),
                journalMode = openParams.journalMode,
                lookasideSlotSize = openParams.lookasideSlotSize,
                lookasideSlotCount = openParams.lookasideSlotCount,
                syncMode = openParams.synchronousMode,
            )
            db.open()
            return db
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
         */
        fun SQLiteDatabase.insert(table: String?, nullColumnHack: String?, values: ContentValues): Long = try {
            insertWithOnConflict(table, nullColumnHack, values, ConflictAlgorithm.CONFLICT_NONE)
        } catch (e: SQLException) {
            logger.e(e) { "Error inserting $values" }
            -1
        }

        /**
         * Gets default connection flags that are appropriate for this thread, taking into
         * account whether the thread is acting on behalf of the UI.
         *
         * @param readOnly True if the connection should be read-only.
         * @return The connection flags.
         */
        fun getThreadDefaultConnectionFlags(readOnly: Boolean): Int {
            return if (readOnly) CONNECTION_FLAG_READ_ONLY else CONNECTION_FLAG_PRIMARY_CONNECTION_AFFINITY
        }

        private fun isCorruptException(throwable: Throwable?): Boolean {
            var th = throwable
            while (th != null) {
                if (th is AndroidSqliteDatabaseCorruptException) {
                    return true
                }
                th = th.cause
            }
            return false
        }
    }
}
