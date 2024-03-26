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
import android.database.sqlite.SQLiteException
import androidx.core.os.CancellationSignal
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion.ENABLE_WRITE_AHEAD_LOGGING
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion.OPEN_READONLY
import ru.pixnews.wasm.sqlite.open.helper.SqliteDatabaseConfiguration
import ru.pixnews.wasm.sqlite.open.helper.common.api.Locale.Companion.EN_US
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.contains
import ru.pixnews.wasm.sqlite.open.helper.common.api.xor
import ru.pixnews.wasm.sqlite.open.helper.internal.CloseGuard.SqliteDatabaseFinalizeAction
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteStatementType.Companion.getSqlStatementType
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteStatementType.Companion.isCacheable
import ru.pixnews.wasm.sqlite.open.helper.internal.WasmSqliteCleaner.WasmSqliteCleanable
import ru.pixnews.wasm.sqlite.open.helper.internal.connection.OperationLog
import ru.pixnews.wasm.sqlite.open.helper.internal.connection.OperationLog.Companion.trimSqlForDisplay
import ru.pixnews.wasm.sqlite.open.helper.internal.connection.PreparedStatement
import ru.pixnews.wasm.sqlite.open.helper.internal.connection.PreparedStatementCache
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.CursorWindow
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.SqlOpenHelperNativeBindings
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3ConnectionPtr
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3StatementPtr
import ru.pixnews.wasm.sqlite.open.helper.toSqliteOpenFlags
import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicBoolean

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
internal class SQLiteConnection<CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr> private constructor(
    private val connectionPtrResource: ConnectionPtrClosable<CP>,
    onConnectionLeaked: () -> Unit,
    configuration: SqliteDatabaseConfiguration,
    private val bindings: SqlOpenHelperNativeBindings<CP, SP>,
    private val connectionId: Int,
    internal val isPrimaryConnection: Boolean,
    private val debugConfig: SQLiteDebug,
    rootLogger: Logger,
) : CancellationSignal.OnCancelListener {
    internal val logger = rootLogger.withTag(TAG)
    private val closeGuard: CloseGuard = CloseGuard.get()
    private val closeGuardCleanable: WasmSqliteCleanable
    private val connectionPtrResourceCleanable: WasmSqliteCleanable
    private val configuration = SqliteDatabaseConfiguration(configuration)
    private val preparedStatementCache = PreparedStatementCache(connectionPtr, bindings, configuration.maxSqlCacheSize)
    private val connectionPtr: CP get() = connectionPtrResource.nativePtr
    internal val databaseLabel: String get() = configuration.label

    // The recent operations log.
    private val recentOperations = OperationLog(debugConfig, logger, System::currentTimeMillis, TimestampFormatter)
    private var onlyAllowReadOnlyOperations = false

    // The number of times attachCancellationSignal has been called.
    // Because SQLite statement execution can be reentrant, we keep track of how many
    // times we have attempted to attach a cancellation signal to the connection so that
    // we can ensure that we detach the signal at the right time.
    private var cancellationSignalAttachCount = 0

    private object TimestampFormatter : (Long) -> String {
        @SuppressLint("SimpleDateFormat")
        private val startTimeDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        override fun invoke(timestamp: Long): String = startTimeDateFormat.format(timestamp)
    }

    init {
        closeGuard.open("close")
        closeGuardCleanable = WasmSqliteCleaner.register(this, SqliteDatabaseFinalizeAction(closeGuard))
        connectionPtrResourceCleanable = WasmSqliteCleaner.register(
            this,
            ConnectionPtrClosableCleanAction(
                ptr = connectionPtrResource,
                onConnectionLeaked = onConnectionLeaked,
                bindings = bindings,
                operationLog = recentOperations,
                preparedStatementCache = preparedStatementCache,
            ),
        )
    }

    private fun configure() {
        if (configuration.isReadOnlyConnection) {
            return
        }
        if (!configuration.isInMemoryDb) {
            setPageSize()
            setJournalSizeLimit()
            setAutoCheckpointInterval()
            setWalMode(configuration.isWalEnabled)
        }
        setForeignKeyMode(configuration.foreignKeyConstraintsEnabled)
        setLocale((configuration.locale ?: EN_US).icuId)
    }

    // Called by SQLiteConnectionPool only.
    fun reconfigure(newConfiguration: SqliteDatabaseConfiguration) {
        onlyAllowReadOnlyOperations = false

        // Remember what changed.
        val foreignKeyModeChanged = newConfiguration.foreignKeyConstraintsEnabled !=
                this.configuration.foreignKeyConstraintsEnabled
        val walModeChanged = (newConfiguration.openFlags xor this.configuration.openFlags)
            .contains(ENABLE_WRITE_AHEAD_LOGGING)
        val localeChanged = newConfiguration.locale != this.configuration.locale

        // Update configuration parameters.
        this.configuration.updateParametersFrom(newConfiguration)

        if (!configuration.isReadOnlyConnection) {
            // Update foreign key mode.
            if (foreignKeyModeChanged) {
                setForeignKeyMode(configuration.foreignKeyConstraintsEnabled)
            }

            // Update WAL.
            if (walModeChanged && !configuration.isInMemoryDb) {
                setWalMode(configuration.isWalEnabled)
            }

            // Update locale.
            if (localeChanged) {
                setLocale((configuration.locale ?: EN_US).icuId)
            }
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
     * This method can be used to check for syntax errors during compilation
     * prior to execution of the statement.  If the `outStatementInfo` argument
     * is not null, the provided [SQLiteStatementInfo] object is populated
     * with information about the statement.
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
        recentOperations.useOperation("prepare", sql) { _ ->
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
        executeOpWithPreparedStatement("execute", sql, bindArgs, cancellationSignal) { statement, _ ->
            bindings.nativeExecute(connectionPtr, statement.statementPtr)
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
        executeOpWithPreparedStatement("executeForLong", sql, bindArgs, cancellationSignal) { statement, _ ->
            return bindings.nativeExecuteForLong(connectionPtr, statement.statementPtr)
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
        executeOpWithPreparedStatement("executeForString", sql, bindArgs, cancellationSignal) { statement, _ ->
            return bindings.nativeExecuteForString(connectionPtr, statement.statementPtr)
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
            executeWithPreparedStatement(sql, bindArgs, cancellationSignal) { statement ->
                changedRows = bindings.nativeExecuteForChangedRowCount(connectionPtr, statement.statementPtr)
                return changedRows
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
    ): Long = executeOpWithPreparedStatement(
        "executeForLastInsertedRowId",
        sql,
        bindArgs,
        cancellationSignal,
    ) { statement, _ ->
        bindings.nativeExecuteForLastInsertedRowId(connectionPtr, statement.statementPtr)
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
    ): Int = window.useReference {
        var actualPos = -1
        var countedRows = -1
        var filledRows = -1
        val cookie = recentOperations.beginOperation("executeForCursorWindow", sql, bindArgs)
        try {
            executeWithPreparedStatement(sql, bindArgs, cancellationSignal) { statement ->
                val result = bindings.nativeExecuteForCursorWindow(
                    connectionPtr = connectionPtr,
                    statementPtr = statement.statementPtr,
                    window = window.window,
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
    }

    // CancellationSignal.OnCancelListener callback.
    // This method may be called on a different thread than the executing statement.
    // However, it will only be called between calls to attachCancellationSignal and
    // detachCancellationSignal, while a statement is executing.  We can safely assume
    // that the SQLite connection is still alive.
    override fun onCancel() = bindings.nativeCancel(connectionPtr)

    // Called by SQLiteConnectionPool.
    // Closes the database closes and releases all of its associated resources.
    // Do not call methods on the connection after it is closed.  It will probably crash.
    fun close() {
        val alreadyClosed = connectionPtrResource.isClosed.getAndSet(true)
        if (!alreadyClosed) {
            closeNativeConnection(
                bindings,
                recentOperations,
                preparedStatementCache,
                connectionPtrResource.nativePtr,
            )
        }
        closeGuard.close()
        connectionPtrResourceCleanable.clean()
        closeGuardCleanable.clean()
    }

    private inline fun <R : Any?> executeOpWithPreparedStatement(
        operationKind: String,
        sql: String,
        bindArgs: List<Any?>,
        cancellationSignal: CancellationSignal?,
        block: (statement: PreparedStatement<SP>, operationCookie: Int) -> R,
    ): R {
        return recentOperations.useOperation(operationKind, sql, bindArgs) { operationCookie ->
            executeWithPreparedStatement(sql, bindArgs, cancellationSignal) { statement ->
                block(statement, operationCookie)
            }
        }
    }

    private inline fun <R : Any?> executeWithPreparedStatement(
        sql: String,
        bindArgs: List<Any?>,
        cancellationSignal: CancellationSignal?,
        block: (statement: PreparedStatement<SP>) -> R,
    ): R {
        val statement = acquirePreparedStatement(sql)
        try {
            throwIfStatementForbidden(statement)
            bindArguments(statement, bindArgs)
            attachCancellationSignal(cancellationSignal)
            try {
                return block(statement)
            } finally {
                detachCancellationSignal(cancellationSignal)
            }
        } finally {
            releasePreparedStatement(statement)
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
            val statementType = getSqlStatementType(sql)
            val putInCache = !skipCache && statementType.isCacheable
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
            bindings.nativeFinalizeStatement(connectionPtr, statement.statementPtr)
        }
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

    override fun toString(): String = "SQLiteConnection: ${configuration.path} ($connectionId)"

    private class ConnectionPtrClosable<CP : Sqlite3ConnectionPtr>(
        val nativePtr: CP,
        val isClosed: AtomicBoolean = AtomicBoolean(false),
    )

    private class ConnectionPtrClosableCleanAction<CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr>(
        val ptr: ConnectionPtrClosable<CP>,
        val onConnectionLeaked: () -> Unit,
        val bindings: SqlOpenHelperNativeBindings<CP, SP>,
        val operationLog: OperationLog,
        val preparedStatementCache: PreparedStatementCache<CP, SP>,
    ) : () -> Unit {
        override fun invoke() {
            val alreadyClosed = ptr.isClosed.getAndSet(true)
            if (!alreadyClosed) {
                // TODO: can run on any thread, do we need to care about thread safety?
                onConnectionLeaked()
                closeNativeConnection(bindings, operationLog, preparedStatementCache, ptr.nativePtr)
            }
        }
    }

    companion object {
        private const val TAG = "SQLiteConnection"

        internal val SqliteDatabaseConfiguration.isReadOnlyConnection: Boolean
            get() = openFlags.contains(OPEN_READONLY)

        internal val SqliteDatabaseConfiguration.isWalEnabled: Boolean
            get() = openFlags.contains(ENABLE_WRITE_AHEAD_LOGGING)

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
            val connectionPtr = bindings.nativeOpen(
                path = configuration.path,
                openFlags = configuration.openFlags.toSqliteOpenFlags(),
                label = configuration.label,
                enableTrace = debugConfig.sqlStatements,
                enableProfile = debugConfig.sqlTime,
            )

            val connection = SQLiteConnection(
                connectionPtrResource = ConnectionPtrClosable(connectionPtr),
                onConnectionLeaked = pool::onConnectionLeaked,
                configuration = configuration,
                bindings = bindings,
                connectionId = connectionId,
                isPrimaryConnection = primaryConnection,
                debugConfig = debugConfig,
                rootLogger = rootLogger,
            )
            try {
                connection.configure()
                return connection
            } catch (ex: SQLiteException) {
                connection.close()
                throw ex
            }
        }

        internal fun <CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr> closeNativeConnection(
            bindings: SqlOpenHelperNativeBindings<CP, SP>,
            recentOperations: OperationLog,
            preparedStatementCache: PreparedStatementCache<CP, SP>,
            nativePtr: CP,
        ) {
            val cookie = recentOperations.beginOperation("close", null)
            try {
                preparedStatementCache.evictAll()
                bindings.nativeClose(nativePtr)
            } finally {
                recentOperations.endOperation(cookie)
            }
        }

        private fun getTypeOfObject(obj: Any?): Int = when (obj) {
            null -> Cursor.FIELD_TYPE_NULL
            is ByteArray -> Cursor.FIELD_TYPE_BLOB
            is Float, is Double -> Cursor.FIELD_TYPE_FLOAT
            is Long, is Int, is Short, is Byte -> Cursor.FIELD_TYPE_INTEGER
            else -> Cursor.FIELD_TYPE_STRING
        }
    }
}
