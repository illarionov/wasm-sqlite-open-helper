/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.internal

/*
 * Original Copyrights:
 * Copyright (C) 2017-2024 requery.io
 * Copyright (C) 2005-2012 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

import android.database.sqlite.SQLiteTransactionListener
import androidx.core.os.CancellationSignal
import at.released.wasm.sqlite.open.helper.internal.cursor.CursorWindow

/**
 * Provides a single client the ability to use a database.
 *
 * <h2>About database sessions</h2>
 *
 * Database access is always performed using a session.  The session
 * manages the lifecycle of transactions and database connections.
 *
 * Sessions can be used to perform both read-only and read-write operations.
 * There is some advantage to knowing when a session is being used for
 * read-only purposes because the connection pool can optimize the use
 * of the available connections to permit multiple read-only operations
 * to execute in parallel whereas read-write operations may need to be serialized.
 *
 * When *Write Ahead Logging (WAL)* is enabled, the database can
 * execute simultaneous read-only and read-write transactions, provided that
 * at most one read-write transaction is performed at a time.  When WAL is not
 * enabled, read-only transactions can execute in parallel but read-write
 * transactions are mutually exclusive.
 *
 * <h2>Ownership and concurrency guarantees</h2>
 *
 * Session objects are not thread-safe.  In fact, session objects are thread-bound.
 * The [SQLiteDatabase] uses a thread-local variable to associate a session
 * with each thread for the use of that thread alone.  Consequently, each thread
 * has its own session object and therefore its own transaction state independent
 * of other threads.
 *
 * A thread has at most one session per database.  This constraint ensures that
 * a thread can never use more than one database connection at a time for a
 * given database.  As the number of available database connections is limited,
 * if a single thread tried to acquire multiple connections for the same database
 * at the same time, it might deadlock.  Therefore we allow there to be only
 * one session (so, at most one connection) per thread per database.
 *
 * <h2>Transactions</h2>
 *
 * There are two kinds of transaction: implicit transactions and explicit
 * transactions.
 *
 * An implicit transaction is created whenever a database operation is requested
 * and there is no explicit transaction currently in progress.  An implicit transaction
 * only lasts for the duration of the database operation in question and then it
 * is ended.  If the database operation was successful, then its changes are committed.
 *
 * An explicit transaction is started by calling [.beginTransaction] and
 * specifying the desired transaction mode.  Once an explicit transaction has begun,
 * all subsequent database operations will be performed as part of that transaction.
 * To end an explicit transaction, first call [.setTransactionSuccessful] if the
 * transaction was successful, then call [.endTransaction].  If the transaction was
 * marked successful, its changes will be committed, otherwise they will be rolled back.
 *
 * Explicit transactions can also be nested.  A nested explicit transaction is
 * started with [.beginTransaction], marked successful with
 * [.setTransactionSuccessful]and ended with [.endTransaction].
 * If any nested transaction is not marked successful, then the entire transaction
 * including all of its nested transactions will be rolled back
 * when the outermost transaction is ended.
 *
 * To improve concurrency, an explicit transaction can be yielded by calling
 * [.yieldTransaction].  If there is contention for use of the database,
 * then yielding ends the current transaction, commits its changes, releases the
 * database connection for use by another session for a little while, and starts a
 * new transaction with the same properties as the original one.
 * Changes committed by [.yieldTransaction] cannot be rolled back.
 *
 * When a transaction is started, the client can provide a [SQLiteTransactionListener]
 * to listen for notifications of transaction-related events.
 *
 *
 * Recommended usage:
 * `<pre>
 * // First, begin the transaction.
 * session.beginTransaction(SQLiteSession.TRANSACTION_MODE_DEFERRED, 0);
 * try {
 * // Then do stuff...
 * session.execute("INSERT INTO ...", null, 0);
 *
 * // As the very last step before ending the transaction, mark it successful.
 * session.setTransactionSuccessful();
 * } finally {
 * // Finally, end the transaction.
 * // This statement will commit the transaction if it was marked successful or
 * // roll it back otherwise.
 * session.endTransaction();
 * }
 * </pre>`
 *
 * <h2>Database connections</h2>
 *
 * A [SQLiteDatabase] can have multiple active sessions at the same
 * time.  Each session acquires and releases connections to the database
 * as needed to perform each requested database transaction.  If all connections
 * are in use, then database transactions on some sessions will block until a
 * connection becomes available.
 *
 * The session acquires a single database connection only for the duration
 * of a single (implicit or explicit) database transaction, then releases it.
 * This characteristic allows a small pool of database connections to be shared
 * efficiently by multiple sessions as long as they are not all trying to perform
 * database transactions at the same time.
 *
 * <h2>Responsiveness</h2>
 *
 * Because there are a limited number of database connections and the session holds
 * a database connection for the entire duration of a database transaction,
 * it is important to keep transactions short.  This is especially important
 * for read-write transactions since they may block other transactions
 * from executing.  Consider calling [.yieldTransaction] periodically
 * during long-running transactions.
 *
 * Another important consideration is that transactions that take too long to
 * run may cause the application UI to become unresponsive.  Even if the transaction
 * is executed in a background thread, the user will get bored and
 * frustrated if the application shows no data for several seconds while
 * a transaction runs.
 *
 *
 * Guidelines:
 *
 *  * Do not perform database transactions on the UI thread.
 *  * Keep database transactions as short as possible.
 *  * Simple queries often run faster than complex queries.
 *  * Measure the performance of your database transactions.
 *  * Consider what will happen when the size of the data set grows.
 * A query that works well on 100 rows may struggle with 10,000.
 *
 * <h2>Reentrance</h2>
 *
 * This class must tolerate reentrant execution of SQLite operations because
 * triggers may call custom SQLite functions that perform additional queries.
 *
 */
internal class SQLiteSession(
    private val connectionPool: SQLiteConnectionPool,
) {
    private var connection: SQLiteConnection? = null
    private var connectionFlags = 0
    private var connectionUseCount = 0
    private var transactionStack: Transaction? = null

    /**
     * Returns true if the session has a transaction in progress.
     */
    fun hasTransaction(): Boolean = transactionStack != null

    /**
     * Returns true if the session has a nested transaction in progress.
     */
    fun hasNestedTransaction(): Boolean = transactionStack != null && transactionStack!!.parent != null

    /**
     * Returns true if the session has an active database connection.
     */
    fun hasConnection(): Boolean = connection != null

    /**
     * Begins a transaction.
     *
     * Transactions may nest.  If the transaction is not in progress,
     * then a database connection is obtained and a new transaction is started.
     * Otherwise, a nested transaction is started.
     *
     * Each call to [.beginTransaction] must be matched exactly by a call
     * to [.endTransaction].  To mark a transaction as successful,
     * call [.setTransactionSuccessful] before calling [.endTransaction].
     * If the transaction is not successful, or if any of its nested
     * transactions were not successful, then the entire transaction will
     * be rolled back when the outermost transaction is ended.
     *
     * @param transactionMode The transaction mode.  One of: [.TRANSACTION_MODE_DEFERRED],
     * [.TRANSACTION_MODE_IMMEDIATE], or [.TRANSACTION_MODE_EXCLUSIVE].
     * Ignored when creating a nested transaction.
     * @param transactionListener The transaction listener, or null if none.
     * @param connectionFlags The connection flags to use if a connection must be
     * acquired by this operation.  Refer to [SQLiteConnectionPool].
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @throws IllegalStateException if [.setTransactionSuccessful] has already been
     * called for the current transaction.
     * @throws IllegalStateException if an error occurs.
     * @throws OperationCanceledException if the operation was canceled.
     * @see .setTransactionSuccessful
     * @see .yieldTransaction
     * @see .endTransaction
     */
    fun beginTransaction(
        transactionMode: Int,
        transactionListener: SQLiteTransactionListener?,
        connectionFlags: Int,
        cancellationSignal: CancellationSignal? = null,
    ) {
        throwIfTransactionMarkedSuccessful()
        beginTransactionUnchecked(transactionMode, transactionListener, connectionFlags, cancellationSignal)
    }

    private fun beginTransactionUnchecked(
        transactionMode: Int,
        transactionListener: SQLiteTransactionListener?,
        connectionFlags: Int,
        cancellationSignal: CancellationSignal?,
    ) {
        cancellationSignal?.throwIfCanceled()

        if (transactionStack == null) {
            acquireConnection(null, connectionFlags, cancellationSignal) // might throw
        }
        try {
            // Set up the transaction such that we can back out safely
            // in case we fail part way.
            if (transactionStack == null) {
                // Execute SQL might throw a runtime exception.
                when (transactionMode) {
                    TRANSACTION_MODE_IMMEDIATE -> connection!!.execute(
                        "BEGIN IMMEDIATE;",
                        emptyList<Any>(),
                        cancellationSignal,
                    ) // might throw
                    TRANSACTION_MODE_EXCLUSIVE -> connection!!.execute(
                        "BEGIN EXCLUSIVE;",
                        emptyList<Any>(),
                        cancellationSignal,
                    ) // might throw
                    TRANSACTION_MODE_DEFERRED -> connection!!.execute(
                        "BEGIN DEFERRED;",
                        emptyList(),
                        cancellationSignal,
                    )

                    else -> {
                        // Per SQLite documentation, this executes in DEFERRED mode.
                        connection!!.execute("BEGIN;", emptyList<Any>(), cancellationSignal)
                    } // might throw
                }
            }

            // Listener might throw a runtime exception.
            if (transactionListener != null) {
                try {
                    transactionListener.onBegin() // might throw
                } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
                    if (transactionStack == null) {
                        connection!!.execute("ROLLBACK;", emptyList(), cancellationSignal) // might throw
                    }
                    throw ex
                }
            }

            // Bookkeeping can't throw, except an OOM, which is just too bad...
            val transaction = Transaction(
                parent = transactionStack,
                mode = transactionMode,
                listener = transactionListener,
                markedSuccessful = false,
                childFailed = false,
            )
            transactionStack = transaction
        } finally {
            if (transactionStack == null) {
                releaseConnection() // might throw
            }
        }
    }

    /**
     * Marks the current transaction as having completed successfully.
     *
     * This method can be called at most once between [.beginTransaction] and
     * [.endTransaction] to indicate that the changes made by the transaction should be
     * committed.  If this method is not called, the changes will be rolled back
     * when the transaction is ended.
     *
     * @throws IllegalStateException if there is no current transaction, or if
     * [.setTransactionSuccessful] has already been called for the current transaction.
     * @see .beginTransaction
     *
     * @see .endTransaction
     */
    fun setTransactionSuccessful() {
        throwIfNoTransaction()
        throwIfTransactionMarkedSuccessful()
        transactionStack!!.markedSuccessful = true
    }

    /**
     * Ends the current transaction and commits or rolls back changes.
     *
     * If this is the outermost transaction (not nested within any other
     * transaction), then the changes are committed if [.setTransactionSuccessful]
     * was called or rolled back otherwise.
     *
     * This method must be called exactly once for each call to [.beginTransaction].
     *
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @throws IllegalStateException if there is no current transaction.
     * @throws SQLiteException if an error occurs.
     * @throws OperationCanceledException if the operation was canceled.
     * @see .beginTransaction
     * @see .setTransactionSuccessful
     * @see .yieldTransaction
     */
    fun endTransaction(cancellationSignal: CancellationSignal? = null) {
        throwIfNoTransaction()
        checkNotNull(connection)

        endTransactionUnchecked(cancellationSignal, false)
    }

    private fun endTransactionUnchecked(
        cancellationSignal: CancellationSignal?,
        yielding: Boolean,
    ) {
        cancellationSignal?.throwIfCanceled()

        val top = checkNotNull(transactionStack)
        var successful = (top.markedSuccessful || yielding) && !top.childFailed

        var listenerException: RuntimeException? = null
        val listener = top.listener
        if (listener != null) {
            try {
                if (successful) {
                    listener.onCommit() // might throw
                } else {
                    listener.onRollback() // might throw
                }
            } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
                listenerException = ex
                successful = false
            }
        }

        transactionStack = top.parent

        val transactionStack = transactionStack
        if (transactionStack != null) {
            if (!successful) {
                transactionStack.childFailed = true
            }
        } else {
            try {
                requireNotNull(connection).let { connection ->
                    if (successful) {
                        connection.execute("COMMIT;", emptyList(), cancellationSignal) // might throw
                    } else {
                        connection.execute("ROLLBACK;", emptyList(), cancellationSignal) // might throw
                    }
                }
            } finally {
                releaseConnection() // might throw
            }
        }

        if (listenerException != null) {
            throw listenerException
        }
    }

    /**
     * Temporarily ends a transaction to let other threads have use of
     * the database.  Begins a new transaction after a specified delay.
     *
     * If there are other threads waiting to acquire connections,
     * then the current transaction is committed and the database
     * connection is released.  After a short delay, a new transaction
     * is started.
     *
     * The transaction is assumed to be successful so far.  Do not call
     * [.setTransactionSuccessful] before calling this method.
     * This method will fail if the transaction has already been marked
     * successful.
     *
     * The changes that were committed by a yield cannot be rolled back later.
     *
     * Before this method was called, there must already have been
     * a transaction in progress.  When this method returns, there will
     * still be a transaction in progress, either the same one as before
     * or a new one if the transaction was actually yielded.
     *
     *
     * This method should not be called when there is a nested transaction
     * in progress because it is not possible to yield a nested transaction.
     * If `throwIfNested` is true, then attempting to yield
     * a nested transaction will throw [IllegalStateException], otherwise
     * the method will return `false` in that case.
     *
     *
     * If there is no nested transaction in progress but a previous nested
     * transaction failed, then the transaction is not yielded (because it
     * must be rolled back) and this method returns `false`.
     *
     *
     * @param sleepAfterYieldDelayMillis A delay time to wait after yielding
     * the database connection to allow other threads some time to run.
     * If the value is less than or equal to zero, there will be no additional
     * delay beyond the time it will take to begin a new transaction.
     * @param throwIfUnsafe If true, then instead of returning false when no
     * transaction is in progress, a nested transaction is in progress, or when
     * the transaction has already been marked successful, throws [IllegalStateException].
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return True if the transaction was actually yielded.
     * @throws IllegalStateException if `throwIfNested` is true and
     * there is no current transaction, there is a nested transaction in progress or
     * if [.setTransactionSuccessful] has already been called for the current transaction.
     * @throws SQLiteException if an error occurs.
     * @throws OperationCanceledException if the operation was canceled.
     * @see .beginTransaction
     *
     * @see .endTransaction
     */
    fun yieldTransaction(
        sleepAfterYieldDelayMillis: Long,
        throwIfUnsafe: Boolean,
        cancellationSignal: CancellationSignal? = null,
    ): Boolean {
        val transaction = transactionStack

        if (throwIfUnsafe) {
            throwIfNoTransaction()
            throwIfTransactionMarkedSuccessful()
            throwIfNestedTransaction()
        } else {
            if (transaction == null || transaction.markedSuccessful || transaction.parent != null) {
                return false
            }
        }
        checkNotNull(connection)

        if (transaction!!.childFailed) {
            return false
        }

        return yieldTransactionUnchecked(sleepAfterYieldDelayMillis, cancellationSignal) // might throw
    }

    private fun yieldTransactionUnchecked(
        sleepAfterYieldDelayMillis: Long,
        cancellationSignal: CancellationSignal?,
    ): Boolean {
        cancellationSignal?.throwIfCanceled()

        if (!connectionPool.shouldYieldConnection(connection!!)) {
            return false
        }

        val transactionMode = transactionStack!!.mode
        val listener = transactionStack!!.listener
        val connectionFlags = connectionFlags
        endTransactionUnchecked(cancellationSignal, true) // might throw

        if (sleepAfterYieldDelayMillis > 0) {
            try {
                Thread.sleep(sleepAfterYieldDelayMillis)
            } catch (ex: InterruptedException) {
                // we have been interrupted, that's all we need to do
            }
        }

        beginTransactionUnchecked(transactionMode, listener, connectionFlags, cancellationSignal) // might throw
        return true
    }

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
     * then it will be stored in the cache for later and reused if possible.
     *
     *
     * @param sql The SQL statement to prepare.
     * @param connectionFlags The connection flags to use if a connection must be
     * acquired by this operation.  Refer to [SQLiteConnectionPool].
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @throws SQLiteException if an error occurs, such as a syntax error.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun prepare(
        sql: String,
        connectionFlags: Int,
        cancellationSignal: CancellationSignal?,
    ): SQLiteStatementInfo {
        cancellationSignal?.throwIfCanceled()
        return useConnection(sql, connectionFlags, cancellationSignal) { prepare(sql) }
    }

    /**
     * Executes a statement that does not return a result.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param connectionFlags The connection flags to use if a connection must be
     * acquired by this operation.  Refer to [SQLiteConnectionPool].
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun execute(
        sql: String,
        bindArgs: List<Any?>,
        connectionFlags: Int,
        cancellationSignal: CancellationSignal?,
    ) {
        if (executeSpecial(sql, connectionFlags, cancellationSignal)) {
            return
        }

        return useConnection(sql, connectionFlags, cancellationSignal) {
            execute(sql, bindArgs, cancellationSignal)
        }
    }

    /**
     * Executes a statement that returns a single `long` result.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param connectionFlags The connection flags to use if a connection must be
     * acquired by this operation.  Refer to [SQLiteConnectionPool].
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The value of the first column in the first row of the result set
     * as a `long`, or zero if none.
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun executeForLong(
        sql: String,
        bindArgs: List<Any?>,
        connectionFlags: Int,
        cancellationSignal: CancellationSignal?,
    ): Long {
        if (executeSpecial(sql, connectionFlags, cancellationSignal)) {
            return 0
        }

        return useConnection(sql, connectionFlags, cancellationSignal) {
            executeForLong(sql, bindArgs, cancellationSignal) // might throw
        }
    }

    /**
     * Executes a statement that returns a single [String] result.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param connectionFlags The connection flags to use if a connection must be
     * acquired by this operation.  Refer to [SQLiteConnectionPool].
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The value of the first column in the first row of the result set
     * as a `String`, or null if none.
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun executeForString(
        sql: String,
        bindArgs: List<Any?>,
        connectionFlags: Int,
        cancellationSignal: CancellationSignal?,
    ): String? {
        if (executeSpecial(sql, connectionFlags, cancellationSignal)) {
            return null
        }

        return useConnection(sql, connectionFlags, cancellationSignal) {
            executeForString(sql, bindArgs, cancellationSignal) // might throw
        }
    }

    /**
     * Executes a statement that returns a count of the number of rows
     * that were changed.  Use for UPDATE or DELETE SQL statements.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param connectionFlags The connection flags to use if a connection must be
     * acquired by this operation.  Refer to [SQLiteConnectionPool].
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The number of rows that were changed.
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun executeForChangedRowCount(
        sql: String,
        bindArgs: List<Any?>,
        connectionFlags: Int,
        cancellationSignal: CancellationSignal?,
    ): Int {
        if (executeSpecial(sql, connectionFlags, cancellationSignal)) {
            return 0
        }
        return useConnection(sql, connectionFlags, cancellationSignal) {
            executeForChangedRowCount(sql, bindArgs, cancellationSignal) // might throw
        }
    }

    /**
     * Executes a statement that returns the row id of the last row inserted
     * by the statement.  Use for INSERT SQL statements.
     *
     * @param sql The SQL statement to execute.
     * @param bindArgs The arguments to bind, or null if none.
     * @param connectionFlags The connection flags to use if a connection must be
     * acquired by this operation.  Refer to [SQLiteConnectionPool].
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The row id of the last row that was inserted, or 0 if none.
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun executeForLastInsertedRowId(
        sql: String,
        bindArgs: List<Any?>,
        connectionFlags: Int,
        cancellationSignal: CancellationSignal?,
    ): Long {
        if (executeSpecial(sql, connectionFlags, cancellationSignal)) {
            return 0
        }

        return useConnection(sql, connectionFlags, cancellationSignal) {
            executeForLastInsertedRowId(sql, bindArgs, cancellationSignal) // might throw
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
     * @param connectionFlags The connection flags to use if a connection must be
     * acquired by this operation.  Refer to [SQLiteConnectionPool].
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The number of rows that were counted during query execution.  Might
     * not be all rows in the result set unless `countAllRows` is true.
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun executeForCursorWindow(
        sql: String,
        bindArgs: List<Any?>,
        window: CursorWindow,
        startPos: Int,
        requiredPos: Int,
        countAllRows: Boolean,
        connectionFlags: Int,
        cancellationSignal: CancellationSignal?,
    ): Int {
        if (executeSpecial(sql, connectionFlags, cancellationSignal)) {
            window.clear()
            return 0
        }

        return useConnection(sql, connectionFlags, cancellationSignal) {
            executeForCursorWindow(
                sql,
                bindArgs,
                window,
                startPos,
                requiredPos,
                countAllRows,
                cancellationSignal,
            )
        }
    }

    /**
     * Performs special reinterpretation of certain SQL statements such as "BEGIN",
     * "COMMIT" and "ROLLBACK" to ensure that transaction state invariants are
     * maintained.
     *
     *
     * This function is mainly used to support legacy apps that perform their
     * own transactions by executing raw SQL rather than calling [.beginTransaction]
     * and the like.
     *
     * @param sql The SQL statement to execute.
     * @param connectionFlags The connection flags to use if a connection must be
     * acquired by this operation.  Refer to [SQLiteConnectionPool].
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return True if the statement was of a special form that was handled here,
     * false otherwise.
     * @throws SQLiteException if an error occurs, such as a syntax error
     * or invalid number of bind arguments.
     * @throws OperationCanceledException if the operation was canceled.
     */
    private fun executeSpecial(
        sql: String,
        connectionFlags: Int,
        cancellationSignal: CancellationSignal?,
    ): Boolean {
        cancellationSignal?.throwIfCanceled()

        val type = SQLiteStatementType.getSqlStatementType(sql)
        return when (type) {
            SQLiteStatementType.STATEMENT_BEGIN -> {
                beginTransaction(TRANSACTION_MODE_EXCLUSIVE, null, connectionFlags, cancellationSignal)
                true
            }

            SQLiteStatementType.STATEMENT_COMMIT -> {
                setTransactionSuccessful()
                endTransaction(cancellationSignal)
                true
            }

            SQLiteStatementType.STATEMENT_ABORT -> {
                endTransaction(cancellationSignal)
                true
            }

            else -> false
        }
    }

    private fun acquireConnection(
        sql: String?,
        connectionFlags: Int,
        cancellationSignal: CancellationSignal?,
    ): SQLiteConnection {
        val connection = connection ?: run {
            assert(connectionUseCount == 0)
            val newConnection = connectionPool.acquireConnection(sql, connectionFlags, cancellationSignal)
            connection = newConnection
            this.connectionFlags = connectionFlags
            newConnection
        }
        connectionUseCount += 1
        return connection
    }

    private fun releaseConnection() {
        val localConnection = checkNotNull(connection)
        assert(connectionUseCount > 0)
        if (--connectionUseCount == 0) {
            try {
                connectionPool.releaseConnection(localConnection) // might throw
            } finally {
                connection = null
            }
        }
    }

    private inline fun <R> useConnection(
        sql: String?,
        connectionFlags: Int,
        cancellationSignal: CancellationSignal?,
        block: SQLiteConnection.() -> R,
    ): R {
        val connection = acquireConnection(sql, connectionFlags, cancellationSignal) // might throw
        return try {
            block(connection)
        } finally {
            releaseConnection()
        }
    }

    private fun throwIfNoTransaction(): Transaction = checkNotNull(transactionStack) {
        "Cannot perform this operation because there is no current transaction."
    }

    private fun throwIfTransactionMarkedSuccessful() {
        val transaction = transactionStack
        check(transaction == null || !transaction.markedSuccessful) {
            "Cannot perform this operation because " +
                    "the transaction has already been marked successful.  The only " +
                    "thing you can do now is call endTransaction()."
        }
    }

    private fun throwIfNestedTransaction() {
        check(!hasNestedTransaction()) {
            "Cannot perform this operation because a nested transaction is in progress."
        }
    }

    private class Transaction(
        var parent: Transaction? = null,
        val mode: Int = 0,
        val listener: SQLiteTransactionListener? = null,
        var markedSuccessful: Boolean = false,
        var childFailed: Boolean = false,
    )

    companion object {
        /**
         * Transaction mode: Deferred.
         *
         *
         * In a deferred transaction, no locks are acquired on the database
         * until the first operation is performed.  If the first operation is
         * read-only, then a `SHARED` lock is acquired, otherwise
         * a `RESERVED` lock is acquired.
         *
         *
         * While holding a `SHARED` lock, this session is only allowed to
         * read but other sessions are allowed to read or write.
         * While holding a `RESERVED` lock, this session is allowed to read
         * or write but other sessions are only allowed to read.
         *
         *
         * Because the lock is only acquired when needed in a deferred transaction,
         * it is possible for another session to write to the database first before
         * this session has a chance to do anything.
         *
         *
         * Corresponds to the SQLite `BEGIN DEFERRED` transaction mode.
         *
         */
        const val TRANSACTION_MODE_DEFERRED: Int = 0

        /**
         * Transaction mode: Immediate.
         *
         *
         * When an immediate transaction begins, the session acquires a
         * `RESERVED` lock.
         *
         *
         * While holding a `RESERVED` lock, this session is allowed to read
         * or write but other sessions are only allowed to read.
         *
         *
         * Corresponds to the SQLite `BEGIN IMMEDIATE` transaction mode.
         *
         */
        const val TRANSACTION_MODE_IMMEDIATE: Int = 1

        /**
         * Transaction mode: Exclusive.
         *
         *
         * When an exclusive transaction begins, the session acquires an
         * `EXCLUSIVE` lock.
         *
         *
         * While holding an `EXCLUSIVE` lock, this session is allowed to read
         * or write but no other sessions are allowed to access the database.
         *
         *
         * Corresponds to the SQLite `BEGIN EXCLUSIVE` transaction mode.
         *
         */
        const val TRANSACTION_MODE_EXCLUSIVE: Int = 2
    }
}
