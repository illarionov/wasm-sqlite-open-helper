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
import androidx.core.os.OperationCanceledException
import ru.pixnews.wasm.sqlite.open.helper.SQLiteDatabaseJournalMode.WAL
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.internal.CloseGuard.CloseGuardFinalizeAction
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteConnectionPool.AcquiredConnectionStatus.DISCARD
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteConnectionPool.AcquiredConnectionStatus.NORMAL
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteConnectionPool.AcquiredConnectionStatus.RECONFIGURE
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteDatabaseConfiguration.Companion.resolveJournalMode
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.SqlOpenHelperNativeBindings
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3ConnectionPtr
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3StatementPtr
import java.io.Closeable
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.LockSupport
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Maintains a pool of active SQLite database connections.
 *
 * At any given time, a connection is either owned by the pool, or it has been
 * acquired by a [SQLiteSession].  When the [SQLiteSession] is
 * finished with the connection it is using, it must return the connection
 * back to the pool.
 *
 * The pool holds strong references to the connections it owns.  However,
 * it only holds *weak references* to the connections that sessions
 * have acquired from it.  Using weak references in the latter case ensures
 * that the connection pool can detect when connections have been improperly
 * abandoned so that it can create new connections to replace them if needed.
 *
 * The connection pool is thread-safe (but the connections themselves are not).
 *
 * <h2>Exception safety</h2>
 *
 * This code attempts to maintain the invariant that opened connections are
 * always owned.  Unfortunately that means it needs to handle exceptions
 * all over to ensure that broken connections get cleaned up.  Most
 * operations invokving SQLite can throw [SQLiteException] or other
 * runtime exceptions.  This is a bit of a pain to deal with because the compiler
 * cannot help us catch missing exception handling code.
 *
 * The general rule for this file: If we are making calls out to
 * [SQLiteConnection] then we must be prepared to handle any
 * runtime exceptions it might throw at us.  Note that out-of-memory
 * is an [Error], not a [RuntimeException].  We don't trouble ourselves
 * handling out of memory because it is hard to do anything at all sensible then
 * and most likely the VM is about to crash.
 *
 */
@Suppress("LargeClass")
internal class SQLiteConnectionPool<CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr> private constructor(
    configuration: SQLiteDatabaseConfiguration,
    private val debugConfig: SQLiteDebug,
    private val bindings: SqlOpenHelperNativeBindings<CP, SP>,
    rootLogger: Logger,
) : Closeable {
    private val logger: Logger = rootLogger.withTag("SQLiteConnectionPool")
    private val closeGuard: CloseGuard = CloseGuard.get()
    private val closeGuardCleaner = WasmSqliteCleaner.register(this, CloseGuardFinalizeAction(closeGuard))
    private val lock = Any()
    private val connectionLeaked = AtomicBoolean()
    private val configuration = SQLiteDatabaseConfiguration(configuration)
    private var maxConnectionPoolSize = 0
    private var isOpen = false
    private var nextConnectionId = 0
    private var connectionWaiterPool: ConnectionWaiter<CP, SP>? = null
    private var connectionWaiterQueue: ConnectionWaiter<CP, SP>? = null

    // Strong references to all available connections.
    private val availableNonPrimaryConnections: MutableList<SQLiteConnection<CP, SP>> = mutableListOf()
    private var availablePrimaryConnection: SQLiteConnection<CP, SP>? = null

    // Weak references to all acquired connections.  The associated value
    // indicates whether the connection must be reconfigured before being
    // returned to the available connection list or discarded.
    // For example, the prepared statement cache size may have changed and
    // need to be updated in preparation for the next client.
    private val acquiredConnections: WeakHashMap<SQLiteConnection<CP, SP>, AcquiredConnectionStatus> = WeakHashMap()
    private val _totalStatementsTime = AtomicLong(0)
    private val _totalStatementsCount = AtomicLong(0)
    val totalStatementsTime: Long get() = _totalStatementsTime.get()
    val totalStatementsCount: Long get() = _totalStatementsCount.get()
    val path: String get() = configuration.path

    // Prepare statement cache statistics
    internal var totalPrepareStatementCacheMiss: Int = 0
    internal var totalPrepareStatements: Int = 0

    // The database schema sequence number.  This counter is incremented every time a schema
    // change is detected.  Every prepared statement records its schema sequence when the
    // statement is created.  The prepared statement is not put back in the cache if the sequence
    // number has changed.  The counter starts at 1, which allows clients to use 0 as a
    // distinguished value.
    private var databaseSeqNum: Long = 1

    init {
        setMaxConnectionPoolSizeLocked()
    }

    // Might throw
    private fun open() {
        // Open the primary connection.
        // This might throw if the database is corrupt.
        availablePrimaryConnection = openConnectionLocked(
            configuration = configuration,
            primaryConnection = true,
            rootLogger = logger,
        ) // might throw

        // Mark the pool as being open for business.
        isOpen = true
        closeGuard.open("SQLiteConnectionPool.close")
    }

    /**
     * Closes the connection pool.
     *
     * When the connection pool is closed, it will refuse all further requests
     * to acquire connections.  All connections that are currently available in
     * the pool are closed immediately.  Any connections that are still in use
     * will be closed as soon as they are returned to the pool.
     *
     * @throws IllegalStateException if the pool has been closed.
     */
    override fun close() {
        closeGuard.close()
        closeGuardCleaner.clean()

        // Close all connections.  We don't need (or want) to do this
        // when finalized because we don't know what state the connections
        // themselves will be in.  The finalizer is really just here for CloseGuard.
        // The connections will take care of themselves when their own finalizers run.
        synchronized(lock) {
            throwIfClosedLocked()
            isOpen = false

            closeAvailableConnectionsAndLogExceptionsLocked()

            val pendingCount = acquiredConnections.size
            if (pendingCount != 0) {
                logger.i {
                    "The connection pool for ${configuration.label} has been closed but there are still " +
                            "$pendingCount connections in use.  They will be closed as they are released back to " +
                            "the pool."
                }
            }
            wakeConnectionWaitersLocked()
        }
    }

    /**
     * Reconfigures the database configuration of the connection pool and all of its
     * connections.
     *
     * Configuration changes are propagated down to connections immediately if
     * they are available or as soon as they are released.  This includes changes
     * that affect the size of the pool.
     *
     *
     * @param configuration The new configuration.
     * @throws IllegalStateException if the pool has been closed.
     */
    fun reconfigure(configuration: SQLiteDatabaseConfiguration): Unit = synchronized(lock) {
        throwIfClosedLocked()
        val isWalCurrentMode = this.configuration.resolveJournalMode() == WAL
        val isWalNewMode = configuration.resolveJournalMode() == WAL
        val walModeChanged = isWalCurrentMode != isWalNewMode
        if (walModeChanged) {
            // WAL mode can only be changed if there are no acquired connections
            // because we need to close all but the primary connection first.
            if (acquiredConnections.isNotEmpty()) {
                throw IllegalStateException(
                    "Write Ahead Logging (WAL) mode cannot " +
                            "be enabled or disabled while there are transactions in " +
                            "progress.  Finish all transactions and release all active " +
                            "database connections first.",
                )
            }

            // Close all non-primary connections.  This should happen immediately
            // because none of them are in use.
            closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked()
            assert(availableNonPrimaryConnections.isEmpty())
        }

        val foreignKeyModeChanged = configuration.foreignKeyConstraintsEnabled !=
                this.configuration.foreignKeyConstraintsEnabled
        if (foreignKeyModeChanged &&
            // Foreign key constraints can only be changed if there are no transactions
            // in progress.  To make this clear, we throw an exception if there are
            // any acquired connections.
            acquiredConnections.isNotEmpty()
        ) {
            throw IllegalStateException(
                "Foreign Key Constraints cannot " +
                        "be enabled or disabled while there are transactions in " +
                        "progress.  Finish all transactions and release all active " +
                        "database connections first.",
            )
        }
        if (this.configuration.openFlags != configuration.openFlags) {
            // If we are changing open flags and WAL mode at the same time, then
            // we have no choice but to close the primary connection beforehand
            // because there can only be one connection open when we change WAL mode.
            if (walModeChanged) {
                closeAvailableConnectionsAndLogExceptionsLocked()
            }

            // Try to reopen the primary connection using the new open flags then
            // close and discard all existing connections.
            // This might throw if the database is corrupt or cannot be opened in
            // the new mode in which case existing connections will remain untouched.
            val newPrimaryConnection = openConnectionLocked(
                configuration = configuration,
                primaryConnection = true,
                rootLogger = logger,
            ) // might throw

            closeAvailableConnectionsAndLogExceptionsLocked()
            discardAcquiredConnectionsLocked()

            availablePrimaryConnection = newPrimaryConnection
            this.configuration.updateParametersFrom(configuration)
            setMaxConnectionPoolSizeLocked()
        } else {
            // Reconfigure the database connections in place.
            this.configuration.updateParametersFrom(configuration)
            setMaxConnectionPoolSizeLocked()

            closeExcessConnectionsAndLogExceptionsLocked()
            reconfigureAllConnectionsLocked()
        }
        wakeConnectionWaitersLocked()
    }

    /**
     * Acquires a connection from the pool.
     *
     * The caller must call [.releaseConnection] to release the connection
     * back to the pool when it is finished.  Failure to do so will result
     * in much unpleasantness.
     *
     * @param sql If not null, try to find a connection that already has
     * the specified SQL statement in its prepared statement cache.
     * @param connectionFlags The connection request flags.
     * @param cancellationSignal A signal to cancel the operation in progress, or null if none.
     * @return The connection that was acquired, never null.
     * @throws IllegalStateException if the pool has been closed.
     * @throws SQLiteException if a database error occurs.
     * @throws OperationCanceledException if the operation was canceled.
     */
    fun acquireConnection(
        sql: String?,
        connectionFlags: Int,
        cancellationSignal: CancellationSignal?,
    ): SQLiteConnection<CP, SP> = waitForConnection(sql, connectionFlags, cancellationSignal)

    /**
     * Releases a connection back to the pool.
     *
     * It is ok to call this method after the pool has closed, to release
     * connections that were still in use at the time of closure.
     *
     * @param connection The connection to release.  Must not be null.
     * @throws IllegalStateException if the connection was not acquired
     * from this pool or if it has already been released.
     */
    fun releaseConnection(connection: SQLiteConnection<CP, SP>): Unit = synchronized(lock) {
        val status = acquiredConnections.remove(connection)
            ?: throw IllegalStateException(
                "Cannot perform this operation " +
                        "because the specified connection was not acquired " +
                        "from this pool or has already been released.",
            )
        if (!isOpen) {
            closeConnectionAndLogExceptionsLocked(connection)
        } else if (connection.isPrimaryConnection) {
            if (recycleConnectionLocked(connection, status)) {
                assert(availablePrimaryConnection == null)
                availablePrimaryConnection = connection
            }
            wakeConnectionWaitersLocked()
        } else if (availableNonPrimaryConnections.size >= maxConnectionPoolSize) {
            closeConnectionAndLogExceptionsLocked(connection)
        } else {
            if (recycleConnectionLocked(connection, status)) {
                availableNonPrimaryConnections.add(connection)
            }
            wakeConnectionWaitersLocked()
        }
    }

    internal fun hasAnyAvailableNonPrimaryConnection(): Boolean = availableNonPrimaryConnections.isNotEmpty()

    // Can't throw.
    private fun recycleConnectionLocked(
        connection: SQLiteConnection<*, *>,
        status: AcquiredConnectionStatus,
    ): Boolean {
        var discard = false
        if (status == RECONFIGURE) {
            try {
                connection.reconfigure(configuration) // might throw
            } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
                logger.e(ex) { "Failed to reconfigure released connection, closing it: $connection" }
                discard = true
            }
        }
        if (status == DISCARD || discard) {
            closeConnectionAndLogExceptionsLocked(connection)
            return false
        }
        return true
    }

    fun clearAcquiredConnectionsPreparedStatementCache() {
        // Invalidate prepared statements that have an earlier schema sequence number.
        synchronized(lock) {
            databaseSeqNum++
            if (acquiredConnections.isNotEmpty()) {
                for (connection in acquiredConnections.keys) {
                    connection.setDatabaseSeqNum(databaseSeqNum)
                }
            }
        }
    }

    /**
     * Returns true if the session should yield the connection due to
     * contention over available database connections.
     *
     * @param connection The connection owned by the session.
     * @return True if the session should yield its connection.
     * @throws IllegalStateException if the connection was not acquired
     * from this pool or if it has already been released.
     */
    fun shouldYieldConnection(connection: SQLiteConnection<*, *>): Boolean =
        synchronized(lock) {
            if (!acquiredConnections.containsKey(connection)) {
                throw IllegalStateException(
                    "Cannot perform this operation " +
                            "because the specified connection was not acquired " +
                            "from this pool or has already been released.",
                )
            }
            return if (isOpen) {
                isSessionBlockingImportantConnectionWaitersLocked(
                    holdingPrimaryConnection = connection.isPrimaryConnection,
                )
            } else {
                false
            }
        }

    // Might throw.
    private fun openConnectionLocked(
        configuration: SQLiteDatabaseConfiguration,
        primaryConnection: Boolean,
        rootLogger: Logger,
    ): SQLiteConnection<CP, SP> {
        val connectionId = nextConnectionId++
        return SQLiteConnection.open(
            pool = this,
            configuration = configuration,
            bindings = bindings,
            connectionId = connectionId,
            primaryConnection = primaryConnection,
            debugConfig = debugConfig,
            onStatementExecuted = ::onStatementExecuted,
            rootLogger = rootLogger,
        ) // might throw
    }

    fun onConnectionLeaked() {
        // This code is running inside of the SQLiteConnection finalizer.
        //
        // We don't know whether it is just the connection that has been finalized (and leaked)
        // or whether the connection pool has also been or is about to be finalized.
        // Consequently, it would be a bad idea to try to grab any locks or to
        // do any significant work here.  So we do the simplest possible thing and
        // set a flag.  waitForConnection() periodically checks this flag (when it
        // times out) so that it can recover from leaked connections and wake
        // itself or other threads up if necessary.
        //
        // You might still wonder why we don't try to do more to wake up the waiters
        // immediately.  First, as explained above, it would be hard to do safely
        // unless we started an extra Thread to function as a reference queue.  Second,
        // this is never supposed to happen in normal operation.  Third, there is no
        // guarantee that the GC will actually detect the leak in a timely manner so
        // it's not all that important that we recover from the leak in a timely manner
        // either.  Fourth, if a badly behaved application finds itself hung waiting for
        // several seconds while waiting for a leaked connection to be detected and recreated,
        // then perhaps its authors will have added incentive to fix the problem!

        logger.w {
            "A SQLiteConnection object for database '" +
                    configuration.label + "' was leaked!  Please fix your application " +
                    "to end transactions in progress properly and to close the database " +
                    "when it is no longer needed."
        }

        connectionLeaked.set(true)
    }

    fun onStatementExecuted(executionTimeMs: Long) {
        _totalStatementsTime.addAndGet(executionTimeMs)
        _totalStatementsCount.incrementAndGet()
    }

    /**
     * Close non-primary connections that are not currently in use. This method is safe to use
     * in finalize block as it doesn't throw RuntimeExceptions.
     */
    fun closeAvailableNonPrimaryConnectionsAndLogExceptions() = synchronized(lock) {
        closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked()
    }

    // Can't throw.
    private fun closeAvailableConnectionsAndLogExceptionsLocked() {
        closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked()
        availablePrimaryConnection?.let {
            closeConnectionAndLogExceptionsLocked(it)
            availablePrimaryConnection = null
        }
    }

    // Can't throw.
    private fun closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked() {
        for (connection in availableNonPrimaryConnections) {
            closeConnectionAndLogExceptionsLocked(connection)
        }
        availableNonPrimaryConnections.clear()
    }

    // Can't throw.
    private fun closeExcessConnectionsAndLogExceptionsLocked() {
        var availableCount = availableNonPrimaryConnections.size
        while (availableCount-- > maxConnectionPoolSize - 1) {
            val connection = availableNonPrimaryConnections.removeAt(availableCount)
            closeConnectionAndLogExceptionsLocked(connection)
        }
    }

    // Can't throw.
    private fun closeConnectionAndLogExceptionsLocked(connection: SQLiteConnection<*, *>) {
        try {
            connection.close() // might throw
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            logger.e(ex) {
                "Failed to close connection, its fate is now in the hands of the merciful GC: $connection"
            }
        }
    }

    // Can't throw.
    private fun discardAcquiredConnectionsLocked() {
        markAcquiredConnectionsLocked(DISCARD)
    }

    // Can't throw.
    private fun reconfigureAllConnectionsLocked() {
        availablePrimaryConnection?.let { connection ->
            try {
                connection.reconfigure(configuration) // might throw
            } catch (@Suppress("SwallowedException", "TooGenericExceptionCaught") ex: RuntimeException) {
                logger.e(ex) { "Failed to reconfigure available primary connection, closing it: $connection" }
                closeConnectionAndLogExceptionsLocked(connection)
                availablePrimaryConnection = null
            }
        }

        var count = availableNonPrimaryConnections.size
        var i = 0
        while (i < count) {
            val connection = availableNonPrimaryConnections[i]
            try {
                connection.reconfigure(configuration) // might throw
            } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
                logger.e(ex) { "Failed to reconfigure available non-primary connection, closing it: $connection" }
                closeConnectionAndLogExceptionsLocked(connection)
                availableNonPrimaryConnections.removeAt(i--)
                count -= 1
            }
            i++
        }

        markAcquiredConnectionsLocked(RECONFIGURE)
    }

    // Can't throw.
    private fun markAcquiredConnectionsLocked(status: AcquiredConnectionStatus) {
        val keysToUpdate = acquiredConnections.mapNotNull { (key, oldStatus) ->
            if (status != oldStatus && oldStatus != DISCARD) {
                key
            } else {
                null
            }
        }
        keysToUpdate.forEach { key -> acquiredConnections[key] = status }
    }

    // Might throw.
    @Suppress("LongMethod")
    private fun waitForConnection(
        sql: String?,
        connectionFlags: Int,
        cancellationSignal: CancellationSignal?,
    ): SQLiteConnection<CP, SP> {
        val wantPrimaryConnection = (connectionFlags and CONNECTION_FLAG_PRIMARY_CONNECTION_AFFINITY) != 0

        val waiter: ConnectionWaiter<CP, SP>
        val nonce: Int
        synchronized(lock) {
            throwIfClosedLocked()
            // Abort if canceled.
            cancellationSignal?.throwIfCanceled()

            // Try to acquire a connection.
            @Suppress("NULLABLE_PROPERTY_TYPE")
            val connection: SQLiteConnection<CP, SP>? = if (!wantPrimaryConnection) {
                tryAcquireNonPrimaryConnectionLocked(sql, connectionFlags) // might throw
            } else {
                null
            } ?: tryAcquirePrimaryConnectionLocked(connectionFlags) // might throw
            if (connection != null) {
                return connection
            }

            // No connections available.  Enqueue a waiter in priority order.
            val startTime = System.nanoTime().nanoseconds.inWholeMilliseconds
            waiter = obtainConnectionWaiterLocked(
                thread = Thread.currentThread(),
                startTime = startTime,
                wantPrimaryConnection = wantPrimaryConnection,
                sql = sql,
                connectionFlags = connectionFlags,
            )
            var predecessor: ConnectionWaiter<CP, SP>? = null
            var successor = connectionWaiterQueue
            while (successor != null) {
                predecessor = successor
                successor = successor.next
            }
            if (predecessor != null) {
                predecessor.next = waiter
            } else {
                connectionWaiterQueue = waiter
            }
            nonce = waiter.nonce
        }

        // Set up the cancellation listener.
        cancellationSignal?.setOnCancelListener {
            synchronized(lock) {
                if (waiter.nonce == nonce) {
                    cancelConnectionWaiterLocked(waiter)
                }
            }
        }
        try {
            // Park the thread until a connection is assigned or the pool is closed.
            // Rethrow an exception from the wait, if we got one.
            var busyTimeout = CONNECTION_POOL_BUSY
            var nextBusyTimeoutTime = waiter.startTime + busyTimeout.inWholeMilliseconds
            while (true) {
                // Detect and recover from connection leaks.
                if (connectionLeaked.compareAndSet(true, false)) {
                    synchronized(lock) {
                        wakeConnectionWaitersLocked()
                    }
                }

                // Wait to be unparked (may already have happened), a timeout, or interruption.
                LockSupport.parkNanos(this, busyTimeout.inWholeNanoseconds)

                // Clear the interrupted flag, just in case.
                Thread.interrupted()

                // Check whether we are done waiting yet.
                synchronized(lock) {
                    throwIfClosedLocked()
                    val connection = waiter.assignedConnection
                    val ex = waiter.exception
                    if (connection != null || ex != null) {
                        recycleConnectionWaiterLocked(waiter)
                        if (connection != null) {
                            return connection
                        }
                        throw (ex)!! // rethrow!
                    }

                    val now = System.nanoTime().nanoseconds.inWholeMilliseconds
                    if (now < nextBusyTimeoutTime) {
                        busyTimeout = (now - nextBusyTimeoutTime).milliseconds
                    } else {
                        logConnectionPoolBusyLocked(now - waiter.startTime, connectionFlags)
                        busyTimeout = CONNECTION_POOL_BUSY
                        nextBusyTimeoutTime = now + busyTimeout.inWholeMilliseconds
                    }
                }
            }
        } finally {
            // Remove the cancellation listener.
            cancellationSignal?.setOnCancelListener(null)
        }
    }

    // Can't throw.
    private fun cancelConnectionWaiterLocked(waiter: ConnectionWaiter<CP, SP>) {
        if (waiter.assignedConnection != null || waiter.exception != null) {
            // Waiter is done waiting but has not woken up yet.
            return
        }

        // Waiter must still be waiting.  Dequeue it.
        var predecessor: ConnectionWaiter<CP, SP>? = null
        var current = connectionWaiterQueue
        while (current != waiter) {
            checkNotNull(current)
            predecessor = current
            current = current.next
        }
        if (predecessor != null) {
            predecessor.next = waiter.next
        } else {
            connectionWaiterQueue = waiter.next
        }

        // Send the waiter an exception and unpark it.
        waiter.exception = OperationCanceledException()
        LockSupport.unpark(waiter.thread)

        // Check whether removing this waiter will enable other waiters to make progress.
        wakeConnectionWaitersLocked()
    }

    // Can't throw.
    private fun logConnectionPoolBusyLocked(waitMillis: Long, connectionFlags: Int) {
        val thread = Thread.currentThread()
        val msg = StringBuilder()
        msg.append("The connection pool for database '").append(configuration.label)
        msg.append("' has been unable to grant a connection to thread ")
        msg.append(thread.id).append(" (").append(thread.name).append(") ")
        msg.append("with flags 0x").append(Integer.toHexString(connectionFlags))
        msg.append(" for ").append(waitMillis * @Suppress("MagicNumber") 0.001f).append(" seconds.\n")

        val requests: MutableList<String> = mutableListOf()
        var activeConnections = 0
        var idleConnections = 0
        if (acquiredConnections.isNotEmpty()) {
            for (connection in acquiredConnections.keys) {
                val description = connection!!.describeCurrentOperationUnsafe()
                if (description != null) {
                    requests.add(description)
                    activeConnections += 1
                } else {
                    idleConnections += 1
                }
            }
        }
        var availableConnections = availableNonPrimaryConnections.size
        if (availablePrimaryConnection != null) {
            availableConnections += 1
        }

        msg.append("Connections: ").append(activeConnections).append(" active, ")
        msg.append(idleConnections).append(" idle, ")
        msg.append(availableConnections).append(" available.\n")

        if (requests.isNotEmpty()) {
            msg.append("\nRequests in progress:\n")
            for (request in requests) {
                msg.append("  ").append(request).append("\n")
            }
        }

        logger.w(message = msg::toString)
    }

    // Can't throw.
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
    private fun wakeConnectionWaitersLocked() {
        // Unpark all waiters that have requests that we can fulfill.
        // This method is designed to not throw runtime exceptions, although we might send
        // a waiter an exception for it to rethrow.
        var predecessor: ConnectionWaiter<CP, SP>? = null
        var waiter = connectionWaiterQueue
        var primaryConnectionNotAvailable = false
        var nonPrimaryConnectionNotAvailable = false
        while (waiter != null) {
            var unpark = false
            if (!isOpen) {
                unpark = true
            } else {
                try {
                    var connection: SQLiteConnection<CP, SP>? = null
                    if (!waiter.wantPrimaryConnection && !nonPrimaryConnectionNotAvailable) {
                        connection = tryAcquireNonPrimaryConnectionLocked(
                            waiter.sql, waiter.connectionFlags,
                        ) // might throw
                        if (connection == null) {
                            nonPrimaryConnectionNotAvailable = true
                        }
                    }
                    if (connection == null && !primaryConnectionNotAvailable) {
                        connection = tryAcquirePrimaryConnectionLocked(
                            waiter.connectionFlags,
                        ) // might throw
                        if (connection == null) {
                            primaryConnectionNotAvailable = true
                        }
                    }
                    if (connection != null) {
                        waiter.assignedConnection = connection
                        unpark = true
                    } else if (nonPrimaryConnectionNotAvailable && primaryConnectionNotAvailable) {
                        // There are no connections available and the pool is still open.
                        // We cannot fulfill any more connection requests, so stop here.
                        break
                    }
                } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
                    // Let the waiter handle the exception from acquiring a connection.
                    waiter.exception = ex
                    unpark = true
                }
            }

            val successor = waiter.next
            if (unpark) {
                if (predecessor != null) {
                    predecessor.next = successor
                } else {
                    connectionWaiterQueue = successor
                }
                waiter.next = null

                LockSupport.unpark(waiter.thread)
            } else {
                predecessor = waiter
            }
            waiter = successor
        }
    }

    // Might throw.
    private fun tryAcquirePrimaryConnectionLocked(connectionFlags: Int): SQLiteConnection<CP, SP>? {
        // If the primary connection is available, acquire it now.
        var connection = availablePrimaryConnection
        if (connection != null) {
            availablePrimaryConnection = null
            finishAcquireConnectionLocked(connection, connectionFlags) // might throw
            return connection
        }

        // Make sure that the primary connection actually exists and has just been acquired.
        for (acquiredConnection in acquiredConnections.keys) {
            if (acquiredConnection.isPrimaryConnection) {
                return null
            }
        }

        // Uhoh.  No primary connection!  Either this is the first time we asked
        // for it, or maybe it leaked?
        connection = openConnectionLocked(
            configuration = configuration,
            primaryConnection = true,
            rootLogger = logger,
        ) // might throw
        finishAcquireConnectionLocked(connection, connectionFlags) // might throw
        return connection
    }

    // Might throw.
    @Suppress("ReturnCount")
    private fun tryAcquireNonPrimaryConnectionLocked(
        sql: String?,
        connectionFlags: Int,
    ): SQLiteConnection<CP, SP>? {
        // Try to acquire the next connection in the queue.
        var connection: SQLiteConnection<CP, SP>
        val availableCount = availableNonPrimaryConnections.size
        if (availableCount > 1 && sql != null) {
            // If we have a choice, then prefer a connection that has the
            // prepared statement in its cache.
            for (i in 0 until availableCount) {
                connection = availableNonPrimaryConnections[i]
                if (connection.isPreparedStatementInCache(sql)) {
                    availableNonPrimaryConnections.removeAt(i)
                    finishAcquireConnectionLocked(connection, connectionFlags) // might throw
                    return connection
                }
            }
        }
        if (availableCount > 0) {
            // Otherwise, just grab the next one.
            connection = availableNonPrimaryConnections.removeAt(availableCount - 1)
            finishAcquireConnectionLocked(connection, connectionFlags) // might throw
            return connection
        }

        // Expand the pool if needed.
        var openConnections = acquiredConnections.size
        if (availablePrimaryConnection != null) {
            openConnections += 1
        }
        if (openConnections >= maxConnectionPoolSize) {
            return null
        }
        connection = openConnectionLocked(
            configuration = configuration,
            primaryConnection = false,
            rootLogger = logger,
        ) // might throw
        finishAcquireConnectionLocked(connection, connectionFlags) // might throw
        return connection
    }

    // Might throw.
    private fun finishAcquireConnectionLocked(connection: SQLiteConnection<CP, SP>, connectionFlags: Int) {
        try {
            val readOnly = (connectionFlags and CONNECTION_FLAG_READ_ONLY) != 0
            connection.setOnlyAllowReadOnlyOperations(readOnly)
            acquiredConnections[connection] = NORMAL
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            logger.e(ex) {
                "Failed to prepare acquired connection for session, closing it: $connection, " +
                        "connectionFlags=$connectionFlags"
            }
            closeConnectionAndLogExceptionsLocked(connection)
            throw ex // rethrow!
        }
    }

    private fun isSessionBlockingImportantConnectionWaitersLocked(
        holdingPrimaryConnection: Boolean,
    ): Boolean {
        var waiter = connectionWaiterQueue ?: return false
        val priority = 0
        @Suppress("LoopWithTooManyJumpStatements")
        do {
            // Only worry about blocked connections that have same or lower priority.
            if (priority > waiter.priority) {
                break
            }

            // If we are holding the primary connection then we are blocking the waiter.
            // Likewise, if we are holding a non-primary connection and the waiter
            // would accept a non-primary connection, then we are blocking the waier.
            if (holdingPrimaryConnection || !waiter.wantPrimaryConnection) {
                return true
            }

            waiter = waiter.next ?: break
        } while (true)
        return false
    }

    private fun setMaxConnectionPoolSizeLocked() {
        maxConnectionPoolSize = if (configuration.resolveJournalMode() == WAL) {
            SQLiteGlobal.WAL_CONNECTION_POOL_SIZE
        } else {
            // We don't actually need to restrict the connection pool size to 1
            // for non-WAL databases.  There might be reasons to use connection pooling
            // with other journal modes. However, we should always keep pool size of 1 for in-memory
            // databases since every :memory: db is separate from another.
            // For now, enabling connection pooling and using WAL are the same thing in the API.
            1
        }
    }

    private fun throwIfClosedLocked() = check(isOpen) {
        "Cannot perform this operation because the connection pool has been closed."
    }

    private fun obtainConnectionWaiterLocked(
        thread: Thread,
        startTime: Long,
        wantPrimaryConnection: Boolean,
        sql: String?,
        connectionFlags: Int,
    ): ConnectionWaiter<CP, SP> {
        var waiter = connectionWaiterPool
        if (waiter != null) {
            connectionWaiterPool = waiter.next
            waiter.next = null
        } else {
            waiter = ConnectionWaiter()
        }
        waiter.thread = thread
        waiter.startTime = startTime
        waiter.wantPrimaryConnection = wantPrimaryConnection
        waiter.sql = sql
        waiter.connectionFlags = connectionFlags
        return waiter
    }

    private fun recycleConnectionWaiterLocked(waiter: ConnectionWaiter<CP, SP>) {
        waiter.next = connectionWaiterPool
        waiter.thread = null
        waiter.sql = null
        waiter.assignedConnection = null
        waiter.exception = null
        waiter.nonce += 1
        connectionWaiterPool = waiter
    }

    internal fun getStatementCacheMissRate(): Double {
        if (totalPrepareStatements == 0) {
            // no statements executed thus no miss rate.
            return 0.0
        }
        return totalPrepareStatementCacheMiss.toDouble() / totalPrepareStatements.toDouble()
    }

    override fun toString(): String = "SQLiteConnectionPool: ${configuration.path}"

    // Describes what should happen to an acquired connection when it is returned to the pool.
    internal enum class AcquiredConnectionStatus {
        // The connection should be returned to the pool as usual.
        NORMAL,

        // The connection must be reconfigured before being returned.
        RECONFIGURE,

        // The connection must be closed and discarded.
        DISCARD,
    }

    private class ConnectionWaiter<CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr> {
        var next: ConnectionWaiter<CP, SP>? = null
        var thread: Thread? = null
        var startTime: Long = 0
        val priority: Int = 0
        var wantPrimaryConnection: Boolean = false
        var sql: String? = null
        var connectionFlags: Int = 0
        var assignedConnection: SQLiteConnection<CP, SP>? = null
        var exception: RuntimeException? = null
        var nonce: Int = 0
    }

    companion object {
        /**
         * Connection flag: Read-only.
         *
         *
         * This flag indicates that the connection will only be used to
         * perform read-only operations.
         *
         */
        const val CONNECTION_FLAG_READ_ONLY: Int = 1

        /**
         * Connection flag: Primary connection affinity.
         *
         *
         * This flag indicates that the primary connection is required.
         * This flag helps support legacy applications that expect most data modifying
         * operations to be serialized by locking the primary database connection.
         * Setting this flag essentially implements the old "db lock" concept by preventing
         * an operation from being performed until it can obtain exclusive access to
         * the primary connection.
         *
         */
        const val CONNECTION_FLAG_PRIMARY_CONNECTION_AFFINITY: Int = 1 shl 1

        // Amount of time to wait in milliseconds before unblocking acquireConnection
        // and logging a message about the connection pool being busy.
        val CONNECTION_POOL_BUSY = 30.seconds

        /**
         * Opens a connection pool for the specified database.
         *
         * @param configuration The database configuration.
         * @return The connection pool.
         * @throws SQLiteException if a database error occurs.
         */
        fun <CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr> open(
            configuration: SQLiteDatabaseConfiguration,
            debugConfig: SQLiteDebug,
            bindings: SqlOpenHelperNativeBindings<CP, SP>,
            logger: Logger,
        ): SQLiteConnectionPool<CP, SP> {
            return SQLiteConnectionPool(configuration, debugConfig, bindings, logger)
                .apply(SQLiteConnectionPool<*, *>::open)
        }
    }
}
