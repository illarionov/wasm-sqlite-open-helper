/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("LargeClass", "VARIABLE_HAS_PREFIX")

package ru.pixnews.wasm.sqlite.open.helper.internal.interop

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr.Companion.SQLITE3_NULL
import ru.pixnews.wasm.sqlite.open.helper.common.api.contains
import ru.pixnews.wasm.sqlite.open.helper.common.api.isSqlite3Null
import ru.pixnews.wasm.sqlite.open.helper.common.api.or
import ru.pixnews.wasm.sqlite.open.helper.common.api.plus
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.EmbedderMemory
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readNullableZeroTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.write
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteMemoryBindings
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.allocZeroTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.sqliteFreeSilent
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.exception.AndroidSqliteCantOpenDatabaseException
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteGlobal.SOFT_HEAP_LIMIT
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.Field.BlobField
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.Field.FloatField
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.Field.IntegerField
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.Field.Null
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.Field.StringField
import ru.pixnews.wasm.sqlite.open.helper.internal.ext.encodedNullTerminatedStringLength
import ru.pixnews.wasm.sqlite.open.helper.internal.ext.rethrowAndroidSqliteException
import ru.pixnews.wasm.sqlite.open.helper.internal.ext.throwAndroidSqliteException
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.JvmSqlOpenHelperNativeBindings.CopyRowResult.CPR_FULL
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.JvmSqlOpenHelperNativeBindings.CopyRowResult.CPR_OK
import ru.pixnews.wasm.sqlite.open.helper.internal.platform.yieldSleepAroundMSec
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType.Companion.SQLITE3_TEXT
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType.Companion.SQLITE_BLOB
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType.Companion.SQLITE_FLOAT
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType.Companion.SQLITE_INTEGER
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType.Companion.SQLITE_NULL
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteConfigParameter
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteConfigParameter.Companion.SQLITE_CONFIG_MULTITHREAD
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDbConfigParameter
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDbConfigParameter.Companion.SQLITE_DBCONFIG_LOOKASIDE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDbStatusParameter
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDbStatusParameter.Companion.SQLITE_DBSTATUS_LOOKASIDE_USED
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDestructorType
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrno
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrno.Companion.SQLITE_DONE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrno.Companion.SQLITE_OK
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrno.Companion.SQLITE_ROW
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrorInfo
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteException
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteException.Companion.sqlite3ErrNoName
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteLogCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteOpenFlags
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteOpenFlags.Companion.SQLITE_OPEN_READWRITE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteStatement
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTrace
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_CLOSE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_PROFILE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_ROW
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_STMT

internal class JvmSqlOpenHelperNativeBindings(
    private val sqliteBindings: SqliteBindings,
    private val memory: EmbedderMemory,
    private val callbackStore: JvmSqliteCallbackStore,
    private val callbackFunctionIndexes: Sqlite3CallbackFunctionIndexes,
    rootLogger: Logger,
) : SqlOpenHelperNativeBindings<GraalSqlite3ConnectionPtr, GraalSqlite3StatementPtr> {
    private val logger = rootLogger.withTag("JvmSqlOpenHelperNativeBindings")
    private val databaseResources: SqliteOpenDatabaseResources = SqliteOpenDatabaseResources(callbackStore, rootLogger)
    private val memoryBindings: SqliteMemoryBindings = sqliteBindings.memoryBindings
    private val connections = Sqlite3ConnectionRegistry()

    override fun nativeInit(
        verboseLog: Boolean,
    ) {
        // Enable multi-threaded mode.  In this mode, SQLite is safe to use by multiple
        // threads as long as no two threads use the same database connection at the same
        // time (which we guarantee in the SQLite database wrappers).
        sqlite3Config(SQLITE_CONFIG_MULTITHREAD, 1)

        // Redirect SQLite log messages to the log.
        val sqliteLogger = logger.withTag("sqlite3")
        sqlite3SetLogger { errCode: Int, message: String -> sqliteLogger.w { "$errCode: $message" } }

        // The soft heap limit prevents the page cache allocations from growing
        // beyond the given limit, no matter what the max page cache sizes are
        // set to. The limit does not, as of 3.5.0, affect any other allocations.
        sqlite3SoftHeapLimit(SOFT_HEAP_LIMIT)

        sqlite3initialize()
    }

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
            db = sqlite3OpenV2(
                filename = path,
                flags = openFlags,
                vfsName = null,
            )

            if (lookasideSlotSize > 0 && lookasideSlotCount > 0) {
                sqlite3DbConfig(
                    db,
                    SQLITE_DBCONFIG_LOOKASIDE,
                    SQLITE3_NULL,
                    lookasideSlotSize,
                    lookasideSlotCount,
                )
            }

            // Check that the database is really read/write when that is what we asked for.
            if (openFlags.contains(SQLITE_OPEN_READWRITE) &&
                sqlite3DbReadonly(db, null) != SqliteDbReadonlyResult.READ_WRITE
            ) {
                throw AndroidSqliteCantOpenDatabaseException("Could not open the database in read/write mode.")
            }

            // Set the default busy handler to retry automatically before returning SQLITE_BUSY.
            sqlite3BusyTimeout(db, BUSY_TIMEOUT_MS)

            // Register custom Android functions.
            registerAndroidFunctions(db)

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
                sqlite3Trace(db, mask, ::sqliteTraceCallback)
            }
            return GraalSqlite3ConnectionPtr(db)
        } catch (e: SqliteException) {
            db?.let {
                sqlite3Close(it)
            }
            e.rethrowAndroidSqliteException()
        } catch (@Suppress("TooGenericExceptionCaught") otherException: RuntimeException) {
            db?.let {
                sqlite3Close(it)
            }
            throw otherException
        }
    }

    override fun nativeRegisterLocalizedCollators(connectionPtr: GraalSqlite3ConnectionPtr, newLocale: String) {
        logger.v { "nativeRegisterLocalizedCollators($connectionPtr, $newLocale)" }
        try {
            registerLocalizedCollators(connectionPtr.ptr, newLocale)
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
            sqlite3Close(connectionPtr.ptr)
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
                sqlite3ProgressHandler(connectionPtr.ptr, 4, ::sqliteProgressHandlerCallback)
            } else {
                sqlite3ProgressHandler(connectionPtr.ptr, 0, null)
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
            val lookasideUsed = sqlite3DbStatus(
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

        val numColumns = sqlite3ColumnCount(statement)
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
                val err = sqlite3Step(statement)
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

            sqlite3Reset(statement) // TODO: check error code, may be SQLITE_BUSY
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

        if (sqlite3Changes(connectionPtr.ptr) <= 0) {
            return -1
        }

        return sqlite3LastInsertRowId(connectionPtr.ptr)
    }

    override fun nativeExecuteForChangedRowCount(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
    ): Int {
        executeNonQuery(connectionPtr, statementPtr, false)
        return sqlite3Changes(connectionPtr.ptr)
    }

    override fun nativeExecuteForString(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
    ): String? {
        executeOneRowQuery(connectionPtr, statementPtr)

        if (sqlite3ColumnCount(statementPtr.ptr) < 1) {
            return null
        }

        return sqlite3ColumnText(statementPtr.ptr, 0)
    }

    override fun nativeExecuteForLong(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
    ): Long {
        executeOneRowQuery(connectionPtr, statementPtr)
        if (sqlite3ColumnCount(statementPtr.ptr) < 1) {
            return -1
        }

        return sqlite3ColumnInt64(statementPtr.ptr, 0)
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
        val err = sqlite3Reset(statementPtr.ptr)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
        if (sqlite3ClearBindings(statementPtr.ptr) != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeBindBlob(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
        value: ByteArray,
    ) {
        val err = sqlite3BindBlobTransient(statementPtr.ptr, index, value)
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
        val err = sqlite3BindStringTransient(statementPtr.ptr, index, value)
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
        val err = sqlite3BindDouble(statementPtr.ptr, index, value)
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
        val err = sqlite3BindLong(statementPtr.ptr, index, value)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeBindNull(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
    ) {
        val err = sqlite3BindNull(statementPtr.ptr, index)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeGetColumnName(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
        index: Int,
    ): String? {
        return sqlite3ColumnName(statementPtr.ptr, index)
    }

    override fun nativeGetColumnCount(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
    ): Int {
        return sqlite3ColumnCount(statementPtr.ptr)
    }

    override fun nativeIsReadOnly(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
    ): Boolean {
        return sqlite3StmtReadonly(statementPtr.ptr)
    }

    override fun nativeGetParameterCount(
        connectionPtr: GraalSqlite3ConnectionPtr,
        statementPtr: GraalSqlite3StatementPtr,
    ): Int {
        return sqlite3BindParameterCount(statementPtr.ptr)
    }

    override fun nativePrepareStatement(
        connectionPtr: GraalSqlite3ConnectionPtr,
        sql: String,
    ): GraalSqlite3StatementPtr {
        try {
            val statementPtr = sqlite3PrepareV2(connectionPtr.ptr, sql)
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
            sqlite3Finalize(connectionPtr.ptr, statementPtr.ptr)
        } catch (sqliteException: SqliteException) {
            logger.v(sqliteException) { "sqlite3_finalize(${connectionPtr.ptr}, ${statementPtr.ptr}) failed" }
        }
    }

    private fun executeNonQuery(
        db: GraalSqlite3ConnectionPtr,
        statement: GraalSqlite3StatementPtr,
        isPragmaStmt: Boolean,
    ) {
        var err = sqlite3Step(statement.ptr)
        if (isPragmaStmt) {
            while (err == SQLITE_ROW) {
                err = sqlite3Step(statement.ptr)
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
        val err = sqlite3Step(statement.ptr)
        if (err != SQLITE_ROW) {
            throwAndroidSqliteException(database.ptr)
        }
    }

    private fun sqliteTraceCallback(trace: SqliteTrace) {
        when (trace) {
            is SqliteTrace.TraceStmt -> logger.d { """${trace.db}: "${trace.unexpandedSql}"""" }
            is SqliteTrace.TraceClose -> logger.d { """${trace.db} closed""" }
            is SqliteTrace.TraceProfile -> logger.d {
                val sql = sqlite3ExpandedSql(trace.statement) ?: trace.statement.toString()
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
        val errInfo = readSqliteErrorInfo(db)
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
                val type = sqlite3ColumnType(statement, columnNo)
                when (type) {
                    SQLITE3_TEXT -> {
                        val text = sqlite3ColumnText(statement, columnNo) ?: run {
                            throwAndroidSqliteException("Null text at ${startPos + addedRows},$columnNo")
                        }
                        val putStatus = window.putField(addedRows, columnNo, StringField(text))
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
                        val value = sqlite3ColumnInt64(statement, columnNo)
                        val putStatus = window.putField(addedRows, columnNo, IntegerField(value))
                        if (putStatus != 0) {
                            logger.v { "Failed allocating space for a long in column $columnNo, error=$putStatus" }
                            result = CPR_FULL
                            break
                        }
                        logger.v { "${startPos + addedRows},$columnNo is INTEGER $value" }
                    }

                    SQLITE_FLOAT -> {
                        val value = sqlite3ColumnDouble(statement, columnNo)
                        val putStatus = window.putField(addedRows, columnNo, FloatField(value))
                        if (putStatus != 0) {
                            logger.v { "Failed allocating space for a double in column $columnNo, error=$putStatus" }
                            result = CPR_FULL
                            break
                        }
                        logger.v { "${startPos + addedRows},$columnNo is FLOAT $value" }
                    }

                    SQLITE_BLOB -> {
                        val value = sqlite3ColumnBlob(statement, columnNo)
                        val putStatus = window.putField(addedRows, columnNo, BlobField(value))
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
                        val putStatus = window.putField(addedRows, columnNo, Null)
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

    private fun sqlite3initialize() {
        val sqliteInitResult = sqliteBindings.sqlite3_initialize.executeForInt()
        if (sqliteInitResult != SqliteErrno.SQLITE_OK.id) {
            throw SqliteException(sqliteInitResult, sqliteInitResult)
        }
    }

    private fun sqlite3OpenV2(
        filename: String,
        flags: SqliteOpenFlags,
        vfsName: String?,
    ): WasmPtr<SqliteDb> {
        var ppDb: WasmPtr<WasmPtr<SqliteDb>> = WasmPtr.sqlite3Null()
        var pFileName: WasmPtr<Byte> = WasmPtr.sqlite3Null()
        var pVfsName: WasmPtr<Byte> = WasmPtr.sqlite3Null()
        var pDb: WasmPtr<SqliteDb> = WasmPtr.sqlite3Null()
        try {
            ppDb = memoryBindings.sqliteAllocOrThrow(WasmPtr.WASM_SIZEOF_PTR)
            pFileName = allocZeroTerminatedString(filename)
            if (vfsName != null) {
                pVfsName = allocZeroTerminatedString(vfsName)
            }

            val result = sqliteBindings.sqlite3_open_v2.executeForInt(
                pFileName.addr,
                ppDb.addr,
                flags.mask.toInt(),
                pVfsName.addr,
            )

            pDb = memory.readPtr(ppDb)
            result.throwOnSqliteError("sqlite3_open_v2() failed", pDb)

            databaseResources.onDbOpened(pDb)

            return pDb
        } catch (@Suppress("TooGenericExceptionCaught") ex: Throwable) {
            sqlite3Close(pDb)
            throw ex
        } finally {
            memoryBindings.sqliteFreeSilent(ppDb)
            memoryBindings.sqliteFreeSilent(pFileName)
            memoryBindings.sqliteFreeSilent(pVfsName)
        }
    }

    private fun sqlite3Close(
        sqliteDb: WasmPtr<SqliteDb>,
    ) {
        try {
            sqliteBindings.sqlite3_close_v2.executeForInt(sqliteDb.addr)
                .throwOnSqliteError("sqlite3_close_v2() failed", sqliteDb)
        } finally {
            databaseResources.afterDbClosed(sqliteDb)
        }
    }

    @Suppress("WRONG_OVERLOADING_FUNCTION_ARGUMENTS")
    private fun sqlite3Config(op: SqliteConfigParameter, arg1: Int) {
        val errNo = sqliteBindings.sqlite3__wasm_config_i.executeForInt(op.id, arg1)
        if (errNo != Errno.SUCCESS.code) {
            throw SqliteException(errNo, errNo, "sqlite3__wasm_config_i() failed")
        }
    }

    private fun sqlite3SetLogger(logger: SqliteLogCallback?) {
        val oldLogger = callbackStore.sqlite3LogCallback
        try {
            callbackStore.sqlite3LogCallback = logger
            val errNo = sqliteBindings.sqlite3__wasm_config_ii.executeForInt(
                SqliteConfigParameter.SQLITE_CONFIG_LOG.id,
                if (logger != null) callbackFunctionIndexes.loggingCallbackFunction.funcId else 0,
                0,
            )
            if (errNo != Errno.SUCCESS.code) {
                throw SqliteException(errNo, errNo, "sqlite3__wasm_config_ii() failed")
            }
        } catch (@Suppress("TooGenericExceptionCaught") ex: Throwable) {
            callbackStore.sqlite3LogCallback = oldLogger
            throw ex
        }
    }

    private fun sqlite3SoftHeapLimit(limit: Long) {
        val errNo = sqliteBindings.sqlite3_soft_heap_limit64.executeForInt(limit)
        if (errNo != Errno.SUCCESS.code) {
            throw SqliteException(errNo, errNo, "sqlite3_soft_heap_limit64() failed")
        }
    }

    private fun sqlite3ErrMsg(
        sqliteDb: WasmPtr<SqliteDb>,
    ): String? {
        val errorAddr: WasmPtr<Byte> = sqliteBindings.sqlite3_errmsg.executeForPtr(sqliteDb.addr)
        return memory.readNullableZeroTerminatedString(errorAddr)
    }

    private fun sqlite3ErrCode(
        sqliteDb: WasmPtr<SqliteDb>,
    ): Int {
        return sqliteBindings.sqlite3_errcode.executeForInt(sqliteDb.addr)
    }

    private fun sqlite3ExtendedErrCode(
        sqliteDb: WasmPtr<SqliteDb>,
    ): Int {
        return sqliteBindings.sqlite3_extended_errcode.executeForInt(sqliteDb.addr)
    }

    private fun sqlite3DbReadonly(
        sqliteDb: WasmPtr<SqliteDb>,
        dbName: String?,
    ): SqliteDbReadonlyResult {
        val pDbName = if (dbName != null) {
            allocZeroTerminatedString(dbName)
        } else {
            WasmPtr.sqlite3Null()
        }

        try {
            val readonlyResultId = sqliteBindings.sqlite3_db_readonly.executeForInt(sqliteDb.addr, pDbName.addr)
            return SqliteDbReadonlyResult.fromId(readonlyResultId)
        } finally {
            memoryBindings.sqliteFreeSilent(pDbName)
        }
    }

    private fun sqlite3BusyTimeout(
        sqliteDb: WasmPtr<SqliteDb>,
        ms: Int,
    ) {
        val result = sqliteBindings.sqlite3_busy_timeout.executeForInt(sqliteDb.addr, ms)
        result.throwOnSqliteError("sqlite3BusyTimeout() failed", sqliteDb)
    }

    private fun sqlite3Trace(
        sqliteDb: WasmPtr<SqliteDb>,
        mask: SqliteTraceEventCode,
        traceCallback: SqliteTraceCallback?,
    ) {
        if (traceCallback != null) {
            callbackStore.sqlite3TraceCallbacks[sqliteDb] = traceCallback
        }

        val errNo = sqliteBindings.sqlite3_trace_v2.executeForInt(
            sqliteDb.addr,
            mask.mask.toInt(),
            if (traceCallback != null) callbackFunctionIndexes.traceFunction.funcId else 0,
            sqliteDb.addr,
        )

        if (traceCallback == null || errNo != Errno.SUCCESS.code) {
            callbackStore.sqlite3TraceCallbacks.remove(sqliteDb)
        }

        errNo.throwOnSqliteError("sqlite3_trace_v2() failed", sqliteDb)
    }

    private fun sqlite3ProgressHandler(
        sqliteDb: WasmPtr<SqliteDb>,
        instructions: Int,
        progressCallback: SqliteProgressCallback?,
    ) {
        @Suppress("NULLABLE_PROPERTY_TYPE")
        val activeCallback: SqliteProgressCallback? = if (instructions >= 1) {
            progressCallback
        } else {
            null
        }

        if (activeCallback != null) {
            callbackStore.sqlite3ProgressCallbacks[sqliteDb] = activeCallback
        }

        val errNo = sqliteBindings.sqlite3_progress_handler.executeForInt(
            sqliteDb.addr,
            instructions,
            if (activeCallback != null) callbackFunctionIndexes.progressFunction.funcId else 0,
            sqliteDb.addr,
        )

        if (activeCallback == null) {
            callbackStore.sqlite3ProgressCallbacks.remove(sqliteDb)
        }

        errNo.throwOnSqliteError("sqlite3ProgressHandler() failed", sqliteDb)
    }

    private fun sqlite3DbStatus(
        sqliteDb: WasmPtr<SqliteDb>,
        op: SqliteDbStatusParameter,
        resetFlag: Boolean,
    ): SqliteDbStatusResult {
        var pCur: WasmPtr<Int> = WasmPtr.sqlite3Null()
        var pHiwtr: WasmPtr<Int> = WasmPtr.sqlite3Null()

        try {
            pCur = memoryBindings.sqliteAllocOrThrow(4U)
            pHiwtr = memoryBindings.sqliteAllocOrThrow(4U)

            val errCode = sqliteBindings.sqlite3_db_status.executeForInt(
                sqliteDb.addr,
                op.id,
                pCur.addr,
                pHiwtr.addr,
                if (resetFlag) 1 else 0,
            )
            errCode.throwOnSqliteError(null, sqliteDb)
            return SqliteDbStatusResult(0, 0)
        } finally {
            memoryBindings.sqliteFreeSilent(pCur)
            memoryBindings.sqliteFreeSilent(pHiwtr)
        }
    }

    private fun sqlite3DbConfig(
        sqliteDb: WasmPtr<SqliteDb>,
        op: SqliteDbConfigParameter,
        pArg1: WasmPtr<*>,
        arg2: Int,
        arg3: Int,
    ) {
        val errCode = sqliteBindings.sqlite3__wasm_db_config_pii.executeForInt(
            sqliteDb.addr,
            op.id,
            pArg1.addr,
            arg2,
            arg3,
        )
        errCode.throwOnSqliteError("sqlite3DbConfig() failed", sqliteDb)
    }

    private fun sqlite3ColumnCount(
        statement: WasmPtr<SqliteStatement>,
    ): Int {
        return sqliteBindings.sqlite3_column_count.executeForInt(statement.addr)
    }

    private fun sqlite3ColumnText(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): String? {
        val ptr: WasmPtr<Byte> = sqliteBindings.sqlite3_column_text.executeForPtr(statement.addr, columnIndex)
        return memory.readNullableZeroTerminatedString(ptr)
    }

    private fun sqlite3ColumnInt64(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): Long {
        return sqliteBindings.sqlite3_column_int64.executeForLong(statement.addr, columnIndex)
    }

    private fun sqlite3ColumnDouble(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): Double {
        return sqliteBindings.sqlite3_column_double.executeForDouble(statement.addr, columnIndex)
    }

    private fun sqlite3ColumnBlob(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): ByteArray {
        val ptr: WasmPtr<Byte> = sqliteBindings.sqlite3_column_text.executeForPtr(
            statement.addr,
            columnIndex,
        )
        val bytes = sqliteBindings.sqlite3_column_bytes.executeForInt(
            statement.addr,
            columnIndex,
        )
        return memory.readBytes(ptr, bytes)
    }

    private fun registerAndroidFunctions(db: WasmPtr<SqliteDb>) {
        sqliteBindings.register_android_functions.executeForInt(
            db.addr,
            0, // utf16Storage
        ).throwOnSqliteError("register_android_functions() failed", db)
    }

    private fun registerLocalizedCollators(ptr: WasmPtr<SqliteDb>, newLocale: String) {
        var pNewLocale: WasmPtr<Byte> = WasmPtr.sqlite3Null()
        try {
            pNewLocale = allocZeroTerminatedString(newLocale)
            val errCode = sqliteBindings.register_localized_collators.executeForInt(
                ptr.addr,
                pNewLocale.addr,
                0, // utf16Storage
            )
            errCode.throwOnSqliteError("register_localized_collators($newLocale) failed", ptr)
        } finally {
            memoryBindings.sqliteFreeSilent(pNewLocale)
        }
    }

    private fun sqlite3Step(
        statement: WasmPtr<SqliteStatement>,
    ): SqliteErrno {
        val errCode = sqliteBindings.sqlite3_step.executeForInt(statement.addr)
        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    private fun sqlite3Reset(
        statement: WasmPtr<SqliteStatement>,
    ): SqliteErrno {
        val errCode = sqliteBindings.sqlite3_reset.executeForInt(statement.addr)
        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    private fun sqlite3ColumnType(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): SqliteColumnType {
        val type = sqliteBindings.sqlite3_column_type.executeForInt(statement.addr, columnIndex)
        return SqliteColumnType(type)
    }

    private fun Int.throwOnSqliteError(
        msgPrefix: String?,
        sqliteDb: WasmPtr<SqliteDb>,
    ) {
        if (this != Errno.SUCCESS.code) {
            val errInfo = readSqliteErrorInfo(sqliteDb)
            throw SqliteException(errInfo, msgPrefix)
        }
    }

    private fun readSqliteErrorInfo(
        sqliteDb: WasmPtr<SqliteDb>,
    ): SqliteErrorInfo {
        if (sqliteDb.isSqlite3Null()) {
            return SqliteErrorInfo(Errno.SUCCESS.code, Errno.SUCCESS.code, null)
        }

        val errCode = sqlite3ErrCode(sqliteDb)
        val extendedErrCode = sqlite3ExtendedErrCode(sqliteDb)
        val errMsg = if (errCode != 0) {
            sqlite3ErrMsg(sqliteDb) ?: "null"
        } else {
            null
        }
        return SqliteErrorInfo(errCode, extendedErrCode, errMsg)
    }

    private fun sqlite3Changes(sqliteDb: WasmPtr<SqliteDb>): Int {
        return sqliteBindings.sqlite3_changes.executeForInt(sqliteDb.addr)
    }

    private fun sqlite3LastInsertRowId(
        sqliteDb: WasmPtr<SqliteDb>,
    ): Long {
        return sqliteBindings.sqlite3_last_insert_rowid.executeForLong(sqliteDb.addr)
    }

    private fun sqlite3ClearBindings(statement: WasmPtr<SqliteStatement>): SqliteErrno {
        val errCode = sqliteBindings.sqlite3_clear_bindings.executeForInt(statement.addr)
        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    private fun sqlite3BindBlobTransient(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
        value: ByteArray,
    ): SqliteErrno {
        val pValue: WasmPtr<Byte> = memoryBindings.sqliteAllocOrThrow(value.size.toUInt())
        memory.write(pValue, value, 0, value.size)
        val errCode = try {
            sqliteBindings.sqlite3_bind_blob.executeForInt(
                sqliteDb.addr,
                index,
                pValue.addr,
                value.size,
                SqliteDestructorType.SQLITE_TRANSIENT.id, // TODO: change to destructor?
            )
        } finally {
            memoryBindings.sqliteFreeSilent(pValue)
        }

        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    private fun sqlite3BindStringTransient(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
        value: String,
    ): SqliteErrno {
        val encoded = value.encodeToByteArray()
        val size = encoded.size

        val pValue: WasmPtr<Byte> = memoryBindings.sqliteAllocOrThrow(size.toUInt())
        memory.write(pValue, encoded, 0, size)
        val errCode = try {
            sqliteBindings.sqlite3_bind_text.executeForInt(
                sqliteDb.addr,
                index,
                pValue.addr,
                size,
                SqliteDestructorType.SQLITE_TRANSIENT.id, // TODO: change to destructor?
            )
        } finally {
            memoryBindings.sqliteFreeSilent(pValue)
        }

        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    private fun sqlite3BindDouble(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
        value: Double,
    ): SqliteErrno {
        val errCode = sqliteBindings.sqlite3_bind_double.executeForInt(
            sqliteDb.addr,
            index,
            value,
        )
        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    private fun sqlite3BindLong(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
        value: Long,
    ): SqliteErrno {
        val errCode = sqliteBindings.sqlite3_bind_int64.executeForInt(
            sqliteDb.addr,
            index,
            value,
        )
        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    private fun sqlite3BindNull(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
    ): SqliteErrno {
        val errCode = sqliteBindings.sqlite3_bind_int64.executeForInt(sqliteDb.addr, index)
        return SqliteErrno.fromErrNoCode(errCode) ?: error("Unknown error code $errCode")
    }

    private fun sqlite3ColumnName(
        statement: WasmPtr<SqliteStatement>,
        index: Int,
    ): String? {
        val ptr: WasmPtr<Byte> = sqliteBindings.sqlite3_column_name.executeForPtr(statement.addr, index)
        return memory.readNullableZeroTerminatedString(ptr)
    }

    private fun sqlite3StmtReadonly(statement: WasmPtr<SqliteStatement>): Boolean {
        return sqliteBindings.sqlite3_stmt_readonly.executeForInt(statement.addr) != 0
    }

    private fun sqlite3BindParameterCount(statement: WasmPtr<SqliteStatement>): Int {
        return sqliteBindings.sqlite3_bind_parameter_count.executeForInt(statement.addr)
    }

    private fun sqlite3PrepareV2(
        sqliteDb: WasmPtr<SqliteDb>,
        sql: String,
    ): WasmPtr<SqliteStatement> {
        var sqlBytesPtr: WasmPtr<Byte> = WasmPtr.sqlite3Null()
        var ppStatement: WasmPtr<WasmPtr<SqliteStatement>> = WasmPtr.sqlite3Null()

        try {
            val sqlEncoded = sql.encodeToByteArray()
            val nullTerminatedSqlSize = sqlEncoded.size + 1

            sqlBytesPtr = memoryBindings.sqliteAllocOrThrow(nullTerminatedSqlSize.toUInt())
            ppStatement = memoryBindings.sqliteAllocOrThrow(WasmPtr.WASM_SIZEOF_PTR)

            memory.write(sqlBytesPtr, sqlEncoded)
            memory.writeByte(sqlBytesPtr + sqlEncoded.size, 0)

            val result = sqliteBindings.sqlite3_prepare_v2.executeForInt(
                sqliteDb.addr,
                sqlBytesPtr.addr,
                nullTerminatedSqlSize,
                ppStatement.addr,
                WasmPtr.sqlite3Null<Unit>().addr,
            )
            result.throwOnSqliteError("sqlite3_prepare_v2() failed", sqliteDb)
            return memory.readPtr(ppStatement).also {
                databaseResources.registerStatement(sqliteDb, it)
            }
        } finally {
            memoryBindings.sqliteFreeSilent(sqlBytesPtr)
            memoryBindings.sqliteFreeSilent(ppStatement)
        }
    }

    private fun sqlite3Finalize(
        sqliteDatabase: WasmPtr<SqliteDb>,
        statement: WasmPtr<SqliteStatement>,
    ) {
        try {
            val errCode = sqliteBindings.sqlite3_finalize.executeForInt(statement.addr)
            errCode.throwOnSqliteError("sqlite3_finalize() failed", sqliteDatabase)
        } finally {
            databaseResources.unregisterStatement(sqliteDatabase, statement)
        }
    }

    private fun sqlite3ExpandedSql(statement: WasmPtr<SqliteStatement>): String? {
        val ptr: WasmPtr<Byte> = sqliteBindings.sqlite3_expanded_sql.executeForPtr(statement.addr)
        return memory.readNullableZeroTerminatedString(ptr)
    }

    private fun allocZeroTerminatedString(string: String): WasmPtr<Byte> = memoryBindings.allocZeroTerminatedString(
        memory = memory,
        string = string,
    )

    internal enum class SqliteDbReadonlyResult(public val id: Int) {
        READ_ONLY(1),
        READ_WRITE(0),
        INVALID_NAME(-1),

        ;

        public companion object {
            public fun fromId(id: Int): SqliteDbReadonlyResult = entries.first { it.id == id }
        }
    }

    internal class SqliteDbStatusResult(
        public val current: Int,
        public val highestInstantaneousValue: Int,
    )

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
