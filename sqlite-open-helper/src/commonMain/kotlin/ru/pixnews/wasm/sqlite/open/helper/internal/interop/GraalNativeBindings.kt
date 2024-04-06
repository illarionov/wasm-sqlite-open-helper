/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.interop

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr.Companion.SQLITE3_NULL
import ru.pixnews.wasm.sqlite.open.helper.common.api.contains
import ru.pixnews.wasm.sqlite.open.helper.common.api.isSqlite3Null
import ru.pixnews.wasm.sqlite.open.helper.common.api.or
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteCapi
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteCapi.SqliteDbReadonlyResult
import ru.pixnews.wasm.sqlite.open.helper.exception.AndroidSqliteCantOpenDatabaseException
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow
import ru.pixnews.wasm.sqlite.open.helper.internal.ext.encodedNullTerminatedStringLength
import ru.pixnews.wasm.sqlite.open.helper.internal.ext.rethrowAndroidSqliteException
import ru.pixnews.wasm.sqlite.open.helper.internal.ext.throwAndroidSqliteException
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.GraalNativeBindings.CopyRowResult.CPR_FULL
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.GraalNativeBindings.CopyRowResult.CPR_OK
import ru.pixnews.wasm.sqlite.open.helper.internal.platform.yieldSleepAroundMSec
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType.Companion.SQLITE3_TEXT
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType.Companion.SQLITE_BLOB
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType.Companion.SQLITE_FLOAT
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType.Companion.SQLITE_INTEGER
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType.Companion.SQLITE_NULL
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDbConfigParameter.Companion.SQLITE_DBCONFIG_LOOKASIDE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDbStatusParameter.Companion.SQLITE_DBSTATUS_LOOKASIDE_USED
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrno
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrno.Companion.SQLITE_DONE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrno.Companion.SQLITE_OK
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrno.Companion.SQLITE_ROW
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteException
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteException.Companion.sqlite3ErrNoName
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteOpenFlags
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteOpenFlags.Companion.SQLITE_OPEN_READWRITE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteStatement
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTrace
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_CLOSE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_PROFILE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_ROW
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_STMT
import kotlin.jvm.JvmInline

@JvmInline
internal value class GraalSqlite3ConnectionPtr(
    val ptr: WasmPtr<SqliteDb>,
) : Sqlite3ConnectionPtr {
    override fun isNull(): Boolean = ptr.isSqlite3Null()
}

@JvmInline
internal value class GraalSqlite3StatementPtr(
    val ptr: WasmPtr<SqliteStatement>,
) : Sqlite3StatementPtr {
    override fun isNull(): Boolean = ptr.isSqlite3Null()
}

internal class GraalNativeBindings(
    private val sqlite3Api: SqliteCapi,
    rootLogger: Logger,
) : SqlOpenHelperNativeBindings<GraalSqlite3ConnectionPtr, GraalSqlite3StatementPtr> {
    private val logger = rootLogger.withTag("GraalNativeBindings")
    private val connections = Sqlite3ConnectionRegistry()

    override fun nativeOpen(
        path: String,
        openFlags: SqliteOpenFlags,
        label: String,
        enableTrace: Boolean,
        enableProfile: Boolean,
        lookasideSlotSize: Int,
        lookasideSlotCount: Int,
    ): GraalSqlite3ConnectionPtr {
        var db: WasmPtr<SqliteDb>? = null
        try {
            db = sqlite3Api.sqlite3OpenV2(
                filename = path,
                flags = openFlags,
                vfsName = null,
            )

            if (lookasideSlotSize > 0 && lookasideSlotCount > 0) {
                sqlite3Api.sqlite3DbConfig(
                    db,
                    SQLITE_DBCONFIG_LOOKASIDE,
                    SQLITE3_NULL,
                    lookasideSlotSize,
                    lookasideSlotCount,
                )
            }

            // Check that the database is really read/write when that is what we asked for.
            if (openFlags.contains(SQLITE_OPEN_READWRITE) &&
                sqlite3Api.sqlite3DbReadonly(db, null) != SqliteDbReadonlyResult.READ_WRITE
            ) {
                throw AndroidSqliteCantOpenDatabaseException("Could not open the database in read/write mode.")
            }

            // Set the default busy handler to retry automatically before returning SQLITE_BUSY.
            sqlite3Api.sqlite3BusyTimeout(db, BUSY_TIMEOUT_MS)

            // Register custom Android functions.
            sqlite3Api.registerAndroidFunctions(db, false)

            // Register wrapper object
            connections.add(db, path)

            // Enable tracing and profiling if requested.
            if (enableTrace || enableProfile) {
                var mask = SqliteTraceEventCode(0U)
                if (enableTrace) {
                    mask = mask or SQLITE_TRACE_STMT or SQLITE_TRACE_ROW or SQLITE_TRACE_CLOSE
                }
                if (enableProfile) {
                    mask = mask or SQLITE_TRACE_PROFILE
                }
                sqlite3Api.sqlite3Trace(db, mask, ::sqliteTraceCallback)
            }
            return GraalSqlite3ConnectionPtr(db)
        } catch (e: SqliteException) {
            // TODO: unregister collation / trace callback / profile callback on close?
            db?.let {
                sqlite3Api.sqlite3Close(it)
            }
            e.rethrowAndroidSqliteException()
        } catch (@Suppress("TooGenericExceptionCaught") otherException: RuntimeException) {
            db?.let {
                sqlite3Api.sqlite3Close(it)
            }
            throw otherException
        }
    }

    override fun nativeRegisterLocalizedCollators(connectionPtr: GraalSqlite3ConnectionPtr, newLocale: String) {
        logger.v { "nativeRegisterLocalizedCollators($connectionPtr, $newLocale)" }
        try {
            sqlite3Api.registerLocalizedCollators(connectionPtr.ptr, newLocale, false)
        } catch (e: SqliteException) {
            logger.i {
                "nativeRegisterLocalizedCollators(${connectionPtr.ptr}, $newLocale) failed: ${e.sqlite3ErrNoName}"
            }
            e.rethrowAndroidSqliteException()
        }
    }

    override fun nativeClose(connectionPtr: GraalSqlite3ConnectionPtr) {
        connections.remove(connectionPtr.ptr)
        try {
            sqlite3Api.sqlite3Close(connectionPtr.ptr)
        } catch (e: SqliteException) {
            // This can happen if sub-objects aren't closed first.  Make sure the caller knows.
            logger.i { "sqlite3_close(${connectionPtr.ptr}) failed: ${e.sqlite3ErrNoName}" }
            e.rethrowAndroidSqliteException("Count not close db.")
        }
    }

    @Suppress("COMPACT_OBJECT_INITIALIZATION")
    override fun nativeResetCancel(connectionPtr: GraalSqlite3ConnectionPtr, cancelable: Boolean) {
        val connection = connections.get(connectionPtr.ptr) ?: run {
            logger.i { "nativeResetCancel(${connectionPtr.ptr}): connection not open" }
            return
        }
        connection.isCancelled = false

        try {
            if (cancelable) {
                sqlite3Api.sqlite3ProgressHandler(connectionPtr.ptr, 4, ::sqliteProgressHandlerCallback)
            } else {
                sqlite3Api.sqlite3ProgressHandler(connectionPtr.ptr, 0, null)
            }
        } catch (e: SqliteException) {
            logger.i { "nativeResetCancel(${connectionPtr.ptr}) failed: ${e.sqlite3ErrNoName}" }
            e.rethrowAndroidSqliteException("Count not close db.")
        }
    }

    @Suppress("COMPACT_OBJECT_INITIALIZATION")
    override fun nativeCancel(connectionPtr: GraalSqlite3ConnectionPtr) {
        val connection = connections.get(connectionPtr.ptr) ?: run {
            logger.i { "nativeResetCancel(${connectionPtr.ptr}): connection not open" }
            return
        }
        connection.isCancelled = true
    }

    override fun nativeGetDbLookaside(connectionPtr: GraalSqlite3ConnectionPtr): Int {
        val connection = connections.get(connectionPtr.ptr) ?: run {
            logger.i { "nativeGetDbLookaside(${connectionPtr.ptr}): connection not open" }
            return -1
        }
        try {
            val lookasideUsed = sqlite3Api.sqlite3DbStatus(
                connection.dbPtr,
                SQLITE_DBSTATUS_LOOKASIDE_USED,
                false,
            )
            return lookasideUsed.current
        } catch (e: SqliteException) {
            logger.i { "nativeGetDbLookaside(${connectionPtr.ptr}) failed: ${e.sqlite3ErrNoName}" }
            return -1
        }
    }

    @Suppress(
        "CyclomaticComplexMethod",
        "LOCAL_VARIABLE_EARLY_DECLARATION",
        "LongMethod",
        "LoopWithTooManyJumpStatements",
        "NAME_SHADOWING",
    )
    override fun nativeExecuteForCursorWindow(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        window: NativeCursorWindow,
        startPos: Int,
        requiredPos: Int,
        countAllRows: Boolean,
    ): Long {
        val statement = statementPtr.ptr

        val status = window.clear()
        if (status != 0) {
            throwAndroidSqliteException("Failed to clear the cursor window")
        }

        val numColumns = sqlite3Api.sqlite3ColumnCount(statement)
        if (window.setNumColumns(numColumns) != 0) {
            throwAndroidSqliteException("Failed to set the cursor window column count")
        }

        var totalRows = 0
        var addedRows = 0

        try {
            var retryCount = 0
            var startPos = startPos
            var windowFull = false
            while (!windowFull || countAllRows) {
                val err = sqlite3Api.sqlite3Step(statement)
                when (err) {
                    SQLITE_DONE -> {
                        logger.v { "Processed all rows" }
                        break
                    }

                    SQLITE_ROW -> {
                        logger.v { "Stepped statement $statement to row $totalRows" }
                        retryCount = 0
                        totalRows += 1

                        // Skip the row if the window is full or we haven't reached the start position yet.
                        if (startPos >= totalRows || windowFull) {
                            continue
                        }
                        var cpr = copyRow(window, statement, numColumns, startPos, addedRows)

                        if (cpr == CPR_FULL && addedRows != 0 && startPos + addedRows <= requiredPos) {
                            // We filled the window before we got to the one row that we really wanted.
                            // Clear the window and start filling it again from here.
                            window.clear()
                            window.setNumColumns(numColumns)
                            startPos += addedRows
                            addedRows = 0
                            cpr = copyRow(window, statement, numColumns, startPos, addedRows)
                        }

                        when (cpr) {
                            CPR_OK -> addedRows += 1
                            CPR_FULL -> windowFull = true
                        }
                    }

                    SqliteErrno.SQLITE_LOCKED, SqliteErrno.SQLITE_BUSY -> {
                        // The table is locked, retry
                        logger.v { "Database locked, retrying" }
                        @Suppress("MagicNumber")
                        if (retryCount > 50) {
                            logger.e { "Bailing on database busy retry" }
                            throwAndroidSqliteException(connectionPtr.ptr, "retrycount exceeded")
                        } else {
                            // Sleep to give the thread holding the lock a chance to finish
                            yieldSleepAroundMSec()
                            retryCount++
                        }
                    }

                    else -> throwAndroidSqliteException(connectionPtr.ptr, "sqlite3Step() failed")
                }
            }
        } catch (exception: SqliteException) {
            exception.rethrowAndroidSqliteException("nativeExecuteForCursorWindow() failed")
        } finally {
            logger.v {
                "Resetting statement $statement after fetching $totalRows rows and adding $addedRows rows" +
                        "to the window in ${window.size - window.freeSpace} bytes"
            }

            sqlite3Api.sqlite3Reset(statement) // TODO: check error code, may be SQLITE_BUSY
        }

        // Report the total number of rows on request.
        if (startPos > totalRows) {
            logger.e { "startPos $startPos > actual rows $totalRows" }
        }

        return (startPos.toLong().shr(32)).or(totalRows.toLong())
    }

    override fun nativeExecuteForLastInsertedRowId(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
    ): Long {
        executeNonQuery(connectionPtr, statementPtr, false)

        if (sqlite3Api.sqlite3Changes(connectionPtr.ptr) <= 0) {
            return -1
        }

        return sqlite3Api.sqlite3LastInsertRowId(connectionPtr.ptr)
    }

    override fun nativeExecuteForChangedRowCount(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
    ): Int {
        executeNonQuery(connectionPtr, statementPtr, false)
        return sqlite3Api.sqlite3Changes(connectionPtr.ptr)
    }

    override fun nativeExecuteForString(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
    ): String? {
        executeOneRowQuery(connectionPtr, statementPtr)

        if (sqlite3Api.sqlite3ColumnCount(statementPtr.ptr) < 1) {
            return null
        }

        return sqlite3Api.sqlite3ColumnText(statementPtr.ptr, 0)
    }

    override fun nativeExecuteForLong(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
    ): Long {
        executeOneRowQuery(connectionPtr, statementPtr)
        if (sqlite3Api.sqlite3ColumnCount(statementPtr.ptr) < 1) {
            return -1
        }

        return sqlite3Api.sqlite3ColumnInt64(statementPtr.ptr, 0)
    }

    override fun nativeExecute(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        isPragmaStmt: Boolean,
    ) {
        executeNonQuery(connectionPtr, statementPtr, isPragmaStmt)
    }

    override fun nativeResetStatementAndClearBindings(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
    ) {
        val err = sqlite3Api.sqlite3Reset(statementPtr.ptr)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
        if (sqlite3Api.sqlite3ClearBindings(statementPtr.ptr) != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeBindBlob(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
        value: ByteArray,
    ) {
        val err = sqlite3Api.sqlite3BindBlobTransient(statementPtr.ptr, index, value)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeBindString(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
        value: String,
    ) {
        val err = sqlite3Api.sqlite3BindStringTransient(statementPtr.ptr, index, value)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeBindDouble(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
        value: Double,
    ) {
        val err = sqlite3Api.sqlite3BindDouble(statementPtr.ptr, index, value)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeBindLong(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
        value: Long,
    ) {
        val err = sqlite3Api.sqlite3BindLong(statementPtr.ptr, index, value)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeBindNull(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
    ) {
        val err = sqlite3Api.sqlite3BindNull(statementPtr.ptr, index)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeGetColumnName(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
    ): String? {
        return sqlite3Api.sqlite3ColumnName(statementPtr.ptr, index)
    }

    override fun nativeGetColumnCount(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
    ): Int {
        return sqlite3Api.sqlite3ColumnCount(statementPtr.ptr)
    }

    override fun nativeIsReadOnly(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
    ): Boolean {
        return sqlite3Api.sqlite3StmtReadonly(statementPtr.ptr)
    }

    override fun nativeGetParameterCount(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
    ): Int {
        return sqlite3Api.sqlite3BindParameterCount(statementPtr.ptr)
    }

    override fun nativePrepareStatement(
        connectionPtr: GraalSqlite3ConnectionPtr,
        sql: String,
    ): GraalSqlite3StatementPtr {
        try {
            val statementPtr = sqlite3Api.sqlite3PrepareV2(connectionPtr.ptr, sql)
            logger.v { "Prepared statement $statementPtr on connection ${connectionPtr.ptr}" }
            return GraalSqlite3StatementPtr(statementPtr)
        } catch (sqliteException: SqliteException) {
            sqliteException.rethrowAndroidSqliteException(", while compiling: $sql")
        }
    }

    override fun nativeFinalizeStatement(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
    ) {
        logger.v { "Finalized statement ${statementPtr.ptr} on connection ${connectionPtr.ptr}" }
        // We ignore the result of sqlite3_finalize because it is really telling us about
        // whether any errors occurred while executing the statement.  The statement itself
        // is always finalized regardless.
        try {
            sqlite3Api.sqlite3Finalize(connectionPtr.ptr, statementPtr.ptr)
        } catch (sqliteException: SqliteException) {
            logger.v(sqliteException) { "sqlite3_finalize(${connectionPtr.ptr}, ${statementPtr.ptr}) failed" }
        }
    }

    private fun executeNonQuery(
        db: GraalSqlite3ConnectionPtr,
        statement: GraalSqlite3StatementPtr,
        isPragmaStmt: Boolean,
    ) {
        var err = sqlite3Api.sqlite3Step(statement.ptr)
        if (isPragmaStmt) {
            while (err == SQLITE_ROW) {
                err = sqlite3Api.sqlite3Step(statement.ptr)
            }
        }
        when (err) {
            SQLITE_ROW -> throwAndroidSqliteException(
                "Queries can be performed using SQLiteDatabase query or rawQuery methods only.",
            )

            SQLITE_DONE -> Unit
            else -> throwAndroidSqliteException(db.ptr)
        }
    }

    private fun executeOneRowQuery(
        database: GraalSqlite3ConnectionPtr,
        statement: GraalSqlite3StatementPtr,
    ) {
        val err = sqlite3Api.sqlite3Step(statement.ptr)
        if (err != SQLITE_ROW) {
            throwAndroidSqliteException(database.ptr)
        }
    }

    private fun sqliteTraceCallback(trace: SqliteTrace) {
        when (trace) {
            is SqliteTrace.TraceStmt -> logger.d { """${trace.db}: "${trace.unexpandedSql}"""" }
            is SqliteTrace.TraceClose -> logger.d { """${trace.db} closed""" }
            is SqliteTrace.TraceProfile -> logger.d {
                val sql = sqlite3Api.sqlite3ExpandedSql(trace.statement) ?: trace.statement.toString()
                """${trace.db}: "$sql" took ${trace.time}"""
            }

            is SqliteTrace.TraceRow -> logger.v { """${trace.db} / ${trace.statement}: Received row""" }
        }
    }

    private fun sqliteProgressHandlerCallback(db: WasmPtr<SqliteDb>): Int {
        val connection = connections.get(db) ?: run {
            logger.i { "sqliteProgressHandlerCallback(${db.addr}): database not open" }
            return -1
        }
        return if (connection.isCancelled) 1 else 0
    }

    private fun throwAndroidSqliteException(
        db: WasmPtr<SqliteDb>,
        message: String? = null,
    ): Nothing {
        val errInfo = sqlite3Api.readSqliteErrorInfo(db)
        throwAndroidSqliteException(errInfo, message)
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    @Throws(SqliteException::class)
    private fun copyRow(
        window: NativeCursorWindow,
        statement: WasmPtr<SqliteStatement>,
        numColumns: Int,
        startPos: Int,
        addedRows: Int,
    ): CopyRowResult {
        val status = window.allocRow()
        if (status != 0) {
            logger.i { "Failed allocating fieldDir at startPos $status row $addedRows" }
            return CPR_FULL
        }
        var result = CPR_OK
        try {
            @Suppress("LoopWithTooManyJumpStatements")
            for (columnNo in 0 until numColumns) {
                val type = sqlite3Api.sqlite3ColumnType(statement, columnNo)
                when (type) {
                    SQLITE3_TEXT -> {
                        val text = sqlite3Api.sqlite3ColumnText(statement, columnNo) ?: run {
                            throwAndroidSqliteException("Null text at ${startPos + addedRows},$columnNo")
                        }
                        val putStatus = window.putString(addedRows, columnNo, text)
                        if (putStatus != 0) {
                            logger.v {
                                "Failed allocating ${text.encodedNullTerminatedStringLength()} bytes for text " +
                                        "at ${startPos + addedRows},$columnNo, error=$putStatus"
                            }
                            result = CPR_FULL
                            break
                        }
                        logger.v {
                            "${startPos + addedRows},$columnNo is TEXT with " +
                                    "${text.encodedNullTerminatedStringLength()} bytes"
                        }
                    }

                    SQLITE_INTEGER -> {
                        val value = sqlite3Api.sqlite3ColumnInt64(statement, columnNo)
                        val putStatus = window.putLong(addedRows, columnNo, value)
                        if (putStatus != 0) {
                            logger.v { "Failed allocating space for a long in column $columnNo, error=$putStatus" }
                            result = CPR_FULL
                            break
                        }
                        logger.v { "${startPos + addedRows},$columnNo is INTEGER $value" }
                    }

                    SQLITE_FLOAT -> {
                        val value = sqlite3Api.sqlite3ColumnDouble(statement, columnNo)
                        val putStatus = window.putDouble(addedRows, columnNo, value)
                        if (putStatus != 0) {
                            logger.v { "Failed allocating space for a double in column $columnNo, error=$putStatus" }
                            result = CPR_FULL
                            break
                        }
                        logger.v { "${startPos + addedRows},$columnNo is FLOAT $value" }
                    }

                    SQLITE_BLOB -> {
                        val value = sqlite3Api.sqlite3ColumnBlob(statement, columnNo)
                        val putStatus = window.putBlob(addedRows, columnNo, value)
                        if (putStatus != 0) {
                            logger.v {
                                "Failed allocating ${value.size} bytes for blob at " +
                                        "${startPos + addedRows},$columnNo, error=$putStatus"
                            }
                            result = CPR_FULL
                            break
                        }
                        logger.v { "${startPos + addedRows},$columnNo is Blob with ${value.size} bytes" }
                    }

                    SQLITE_NULL -> {
                        val putStatus = window.putNull(addedRows, columnNo)
                        if (putStatus != 0) {
                            logger.v {
                                "Failed allocating space for a null in column $columnNo, error=$putStatus" +
                                        "${startPos + addedRows},$columnNo, error=$putStatus"
                            }
                            result = CPR_FULL
                            break
                        }
                    }

                    else -> {
                        logger.e { "Unknown column type when filling database window" }
                        throwAndroidSqliteException("Unknown column type when filling window")
                    }
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
            window.freeLastRow()
            throw e
        }

        if (result != CPR_OK) {
            window.freeLastRow()
        }

        return result
    }

    internal class Sqlite3Connection(
        val dbPtr: WasmPtr<SqliteDb>,
        val path: String,
        var isCancelled: Boolean = false,
    )

    private class Sqlite3ConnectionRegistry {
        private val map: MutableMap<WasmPtr<SqliteDb>, Sqlite3Connection> = mutableMapOf()

        fun add(
            dbPtr: WasmPtr<SqliteDb>,
            path: String,
        ): Sqlite3Connection {
            val connection = Sqlite3Connection(dbPtr, path, false)
            val old = map.put(dbPtr, connection)
            check(old == null) { "Connection $dbPtr already registered" }
            return connection
        }

        fun get(ptr: WasmPtr<SqliteDb>): Sqlite3Connection? = map[ptr]

        fun remove(ptr: WasmPtr<SqliteDb>): Sqlite3Connection? = map.remove(ptr)
    }

    private enum class CopyRowResult {
        CPR_OK,
        CPR_FULL,
    }

    internal companion object {
        /* Busy timeout in milliseconds.
        If another connection (possibly in another process) has the database locked for
        longer than this amount of time then SQLite will generate a SQLITE_BUSY error.
        The SQLITE_BUSY error is then raised as a SQLiteDatabaseLockedException.

        In ordinary usage, busy timeouts are quite rare.  Most databases only ever
        have a single open connection at a time unless they are using WAL.  When using
        WAL, a timeout could occur if one connection is busy performing an auto-checkpoint
        operation.  The busy timeout needs to be long enough to tolerate slow I/O write
        operations but not so long as to cause the application to hang indefinitely if
        there is a problem acquiring a database lock. */
        const val BUSY_TIMEOUT_MS = 2500
    }
}
