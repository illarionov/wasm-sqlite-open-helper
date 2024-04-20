/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("LargeClass")

package ru.pixnews.wasm.sqlite.open.helper.internal.interop

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr.Companion.SQLITE3_NULL
import ru.pixnews.wasm.sqlite.open.helper.common.api.contains
import ru.pixnews.wasm.sqlite.open.helper.common.api.or
import ru.pixnews.wasm.sqlite.open.helper.exception.AndroidSqliteCantOpenDatabaseException
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
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.capi.Sqlite3CApi
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.capi.Sqlite3DbFunctions.SqliteDbReadonlyResult
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.capi.Sqlite3ErrorFunctions.Companion.readSqliteErrorInfo
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.capi.Sqlite3Result
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.capi.Sqlite3Result.Error
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.capi.Sqlite3Result.Success
import ru.pixnews.wasm.sqlite.open.helper.internal.platform.yieldSleepAroundMSec
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType.Companion.SQLITE3_TEXT
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType.Companion.SQLITE_BLOB
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType.Companion.SQLITE_FLOAT
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType.Companion.SQLITE_INTEGER
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType.Companion.SQLITE_NULL
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteConfigParameter.Companion.SQLITE_CONFIG_MULTITHREAD
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDbConfigParameter.Companion.SQLITE_DBCONFIG_LOOKASIDE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDbStatusParameter.Companion.SQLITE_DBSTATUS_LOOKASIDE_USED
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrorInfo
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteException
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteOpenFlags
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteOpenFlags.Companion.SQLITE_OPEN_READWRITE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode.Companion.SQLITE_DONE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode.Companion.SQLITE_OK
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode.Companion.SQLITE_ROW
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode.Companion.name
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteStatement
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTrace
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_CLOSE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_PROFILE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_ROW
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_STMT

internal class JvmSqlOpenHelperNativeBindings(
    private val cApi: Sqlite3CApi,
    rootLogger: Logger,
) : SqlOpenHelperNativeBindings<WasmSqlite3ConnectionPtr, WasmSqlite3StatementPtr> {
    private val logger = rootLogger.withTag("JvmSqlOpenHelperNativeBindings")
    private val connections = Sqlite3ConnectionRegistry()

    override fun nativeInit(
        verboseLog: Boolean,
    ) {
        // Enable multi-threaded mode.  In this mode, SQLite is safe to use by multiple
        // threads as long as no two threads use the same database connection at the same
        // time (which we guarantee in the SQLite database wrappers).
        cApi.config.sqlite3Config(SQLITE_CONFIG_MULTITHREAD, 1)
            .throwOnSqliteError("sqlite3__wasm_config_i() failed")

        // Redirect SQLite log messages to the log.
        val sqliteLogger = logger.withTag("sqlite3")
        cApi.config.sqlite3SetLogger { errCode, message -> sqliteLogger.w { "$errCode: $message" } }
            .throwOnSqliteError("sqliteSetLogger() failed")

        // The soft heap limit prevents the page cache allocations from growing
        // beyond the given limit, no matter what the max page cache sizes are
        // set to. The limit does not, as of 3.5.0, affect any other allocations.
        cApi.config.sqlite3SoftHeapLimit(SOFT_HEAP_LIMIT)
            .throwOnSqliteError("sqlite3_soft_heap_limit64() failed")

        cApi.config.sqlite3initialize()
            .throwOnSqliteError("sqlite3_initialize failed")
    }

    @Suppress("CyclomaticComplexMethod")
    override fun nativeOpen(
        path: String,
        openFlags: SqliteOpenFlags,
        label: String,
        enableTrace: Boolean,
        enableProfile: Boolean,
        lookasideSlotSize: Int,
        lookasideSlotCount: Int,
    ): WasmSqlite3ConnectionPtr {
        var db: WasmPtr<SqliteDb>? = null
        try {
            db = cApi.db.sqlite3OpenV2(
                filename = path,
                flags = openFlags,
                vfsName = null,
            ).orThrow("sqlite3OpenV2() failed")

            if (lookasideSlotSize > 0 && lookasideSlotCount > 0) {
                cApi.db.sqlite3DbConfig(
                    db,
                    SQLITE_DBCONFIG_LOOKASIDE,
                    SQLITE3_NULL,
                    lookasideSlotSize,
                    lookasideSlotCount,
                ).orThrow("sqlite3DbConfig() failed")
            }

            // Check that the database is really read/write when that is what we asked for.
            if (openFlags.contains(SQLITE_OPEN_READWRITE) &&
                cApi.db.sqlite3DbReadonly(db, null) != SqliteDbReadonlyResult.READ_WRITE
            ) {
                throw AndroidSqliteCantOpenDatabaseException("Could not open the database in read/write mode.")
            }

            // Set the default busy handler to retry automatically before returning SQLITE_BUSY.
            cApi.db.sqlite3BusyTimeout(db, BUSY_TIMEOUT_MS)
                .orThrow("sqlite3BusyTimeout() failed")

            // Register custom Android functions.
            cApi.db.registerAndroidFunctions(db)
                .orThrow("register_android_functions() failed failed")

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
                cApi.db.sqlite3Trace(db, mask, ::sqliteTraceCallback)
            }
            return WasmSqlite3ConnectionPtr(db)
        } catch (e: SqliteException) {
            closeDatabaseSilent(db)
            e.rethrowAndroidSqliteException()
        } catch (@Suppress("TooGenericExceptionCaught") otherException: RuntimeException) {
            closeDatabaseSilent(db)
            throw otherException
        }
    }

    private fun closeDatabaseSilent(db: WasmPtr<SqliteDb>?) {
        db?.let { database ->
            val closeError = cApi.db.sqlite3Close(database)
            if (closeError != SQLITE_OK) {
                logger.e { "sqlite3Close() failed with error code `${closeError.name}`" }
            }
        }
    }

    override fun nativeRegisterLocalizedCollators(connectionPtr: WasmSqlite3ConnectionPtr, newLocale: String) {
        logger.v { "nativeRegisterLocalizedCollators($connectionPtr, $newLocale)" }
        cApi.db.registerLocalizedCollators(connectionPtr.ptr, newLocale)
            .orThrow("register_localized_collators() failed")
    }

    override fun nativeClose(connectionPtr: WasmSqlite3ConnectionPtr) {
        connections.remove(connectionPtr.ptr)
        val errCode: SqliteResultCode = cApi.db.sqlite3Close(connectionPtr.ptr)
        if (errCode != SQLITE_OK) {
            // This can happen if sub-objects aren't closed first.  Make sure the caller knows.
            logger.i { "sqlite3_close(${connectionPtr.ptr}) failed: ${errCode.name}" }
            errCode.throwOnSqliteError("Count not close db.")
        }
    }

    @Suppress("COMPACT_OBJECT_INITIALIZATION")
    override fun nativeResetCancel(connectionPtr: WasmSqlite3ConnectionPtr, cancelable: Boolean) {
        val connection = connections.get(connectionPtr.ptr) ?: run {
            logger.i { "nativeResetCancel(${connectionPtr.ptr}): connection not open" }
            return
        }
        connection.isCancelled = false

        val result = if (cancelable) {
            cApi.db.sqlite3ProgressHandler(connectionPtr.ptr, 4, ::sqliteProgressHandlerCallback)
        } else {
            cApi.db.sqlite3ProgressHandler(connectionPtr.ptr, 0, null)
        }
        if (result is Error) {
            logger.i { "nativeResetCancel(${connectionPtr.ptr}) failed: ${result.info}" }
        }
        result.orThrow("Count not close db.")
    }

    @Suppress("COMPACT_OBJECT_INITIALIZATION")
    override fun nativeCancel(connectionPtr: WasmSqlite3ConnectionPtr) {
        val connection = connections.get(connectionPtr.ptr) ?: run {
            logger.i { "nativeResetCancel(${connectionPtr.ptr}): connection not open" }
            return
        }
        connection.isCancelled = true
    }

    override fun nativeGetDbLookaside(connectionPtr: WasmSqlite3ConnectionPtr): Int {
        val connection = connections.get(connectionPtr.ptr) ?: run {
            logger.i { "nativeGetDbLookaside(${connectionPtr.ptr}): connection not open" }
            return -1
        }
        val lookasideUsedResult = cApi.db.sqlite3DbStatus(
            connection.dbPtr,
            SQLITE_DBSTATUS_LOOKASIDE_USED,
            false,
        )
        return when (lookasideUsedResult) {
            is Success -> lookasideUsedResult.value.current
            is Error -> {
                logger.i { "nativeGetDbLookaside(${connectionPtr.ptr}) failed: ${lookasideUsedResult.info}" }
                -1
            }
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
        connectionPtr: WasmSqlite3ConnectionPtr,
        statementPtr: WasmSqlite3StatementPtr,
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

        val numColumns = cApi.statement.sqlite3ColumnCount(statement)
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
                val err = cApi.statement.sqlite3Step(statement)
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

                    SqliteResultCode.SQLITE_LOCKED, SqliteResultCode.SQLITE_BUSY -> {
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

            cApi.statement.sqlite3Reset(statement) // TODO: check error code, may be SQLITE_BUSY
        }

        // Report the total number of rows on request.
        if (startPos > totalRows) {
            logger.e { "startPos $startPos > actual rows $totalRows" }
        }

        return (startPos.toLong().shr(32)).or(totalRows.toLong())
    }

    override fun nativeExecuteForLastInsertedRowId(
        connectionPtr: WasmSqlite3ConnectionPtr,
        statementPtr: WasmSqlite3StatementPtr,
    ): Long {
        executeNonQuery(connectionPtr, statementPtr, false)

        if (cApi.db.sqlite3Changes(connectionPtr.ptr) <= 0) {
            return -1
        }

        return cApi.db.sqlite3LastInsertRowId(connectionPtr.ptr)
    }

    override fun nativeExecuteForChangedRowCount(
        connectionPtr: WasmSqlite3ConnectionPtr,
        statementPtr: WasmSqlite3StatementPtr,
    ): Int {
        executeNonQuery(connectionPtr, statementPtr, false)
        return cApi.db.sqlite3Changes(connectionPtr.ptr)
    }

    override fun nativeExecuteForString(
        connectionPtr: WasmSqlite3ConnectionPtr,
        statementPtr: WasmSqlite3StatementPtr,
    ): String? {
        executeOneRowQuery(connectionPtr, statementPtr)

        if (cApi.statement.sqlite3ColumnCount(statementPtr.ptr) < 1) {
            return null
        }

        return cApi.statement.sqlite3ColumnText(statementPtr.ptr, 0)
    }

    override fun nativeExecuteForLong(
        connectionPtr: WasmSqlite3ConnectionPtr,
        statementPtr: WasmSqlite3StatementPtr,
    ): Long {
        executeOneRowQuery(connectionPtr, statementPtr)
        if (cApi.db.sqlite3LastInsertRowId(connectionPtr.ptr) < 1) {
            return -1
        }

        return cApi.statement.sqlite3ColumnInt64(statementPtr.ptr, 0)
    }

    override fun nativeExecute(
        connectionPtr: WasmSqlite3ConnectionPtr,
        statementPtr: WasmSqlite3StatementPtr,
        isPragmaStmt: Boolean,
    ) {
        executeNonQuery(connectionPtr, statementPtr, isPragmaStmt)
    }

    override fun nativeResetStatementAndClearBindings(
        connectionPtr: WasmSqlite3ConnectionPtr,
        statementPtr: WasmSqlite3StatementPtr,
    ) {
        val err = cApi.statement.sqlite3Reset(statementPtr.ptr)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
        if (cApi.statement.sqlite3ClearBindings(statementPtr.ptr) != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeBindBlob(
        connectionPtr: WasmSqlite3ConnectionPtr,
        statementPtr: WasmSqlite3StatementPtr,
        index: Int,
        value: ByteArray,
    ) {
        val err = cApi.statement.sqlite3BindBlobTransient(statementPtr.ptr, index, value)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeBindString(
        connectionPtr: WasmSqlite3ConnectionPtr,
        statementPtr: WasmSqlite3StatementPtr,
        index: Int,
        value: String,
    ) {
        val err = cApi.statement.sqlite3BindStringTransient(statementPtr.ptr, index, value)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeBindDouble(
        connectionPtr: WasmSqlite3ConnectionPtr,
        statementPtr: WasmSqlite3StatementPtr,
        index: Int,
        value: Double,
    ) {
        val err = cApi.statement.sqlite3BindDouble(statementPtr.ptr, index, value)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeBindLong(
        connectionPtr: WasmSqlite3ConnectionPtr,
        statementPtr: WasmSqlite3StatementPtr,
        index: Int,
        value: Long,
    ) {
        val err = cApi.statement.sqlite3BindLong(statementPtr.ptr, index, value)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeBindNull(
        connectionPtr: WasmSqlite3ConnectionPtr,
        statementPtr: WasmSqlite3StatementPtr,
        index: Int,
    ) {
        val err = cApi.statement.sqlite3BindNull(statementPtr.ptr, index)
        if (err != SQLITE_OK) {
            throwAndroidSqliteException(connectionPtr.ptr)
        }
    }

    override fun nativeGetColumnName(
        connectionPtr: WasmSqlite3ConnectionPtr,
        statementPtr: WasmSqlite3StatementPtr,
        index: Int,
    ): String? {
        return cApi.statement.sqlite3ColumnName(statementPtr.ptr, index)
    }

    override fun nativeGetColumnCount(
        connectionPtr: WasmSqlite3ConnectionPtr,
        statementPtr: WasmSqlite3StatementPtr,
    ): Int {
        return cApi.statement.sqlite3ColumnCount(statementPtr.ptr)
    }

    override fun nativeIsReadOnly(
        connectionPtr: WasmSqlite3ConnectionPtr,
        statementPtr: WasmSqlite3StatementPtr,
    ): Boolean {
        return cApi.statement.sqlite3StmtReadonly(statementPtr.ptr)
    }

    override fun nativeGetParameterCount(
        connectionPtr: WasmSqlite3ConnectionPtr,
        statementPtr: WasmSqlite3StatementPtr,
    ): Int {
        return cApi.statement.sqlite3BindParameterCount(statementPtr.ptr)
    }

    override fun nativePrepareStatement(
        connectionPtr: WasmSqlite3ConnectionPtr,
        sql: String,
    ): WasmSqlite3StatementPtr {
        when (val statementPtr = cApi.statement.sqlite3PrepareV2(connectionPtr.ptr, sql)) {
            is Success -> {
                logger.v { "Prepared statement ${statementPtr.value}. on connection ${connectionPtr.ptr}" }
                return WasmSqlite3StatementPtr(statementPtr.value)
            }

            is Error -> throwAndroidSqliteException(connectionPtr.ptr, ", while compiling: $sql")
        }
    }

    override fun nativeFinalizeStatement(
        connectionPtr: WasmSqlite3ConnectionPtr,
        statementPtr: WasmSqlite3StatementPtr,
    ) {
        logger.v { "Finalized statement ${statementPtr.ptr} on connection ${connectionPtr.ptr}" }
        // We ignore the result of sqlite3_finalize because it is really telling us about
        // whether any errors occurred while executing the statement.  The statement itself
        // is always finalized regardless.
        val result = cApi.statement.sqlite3Finalize(connectionPtr.ptr, statementPtr.ptr)
        if (result is Error) {
            logger.v { "sqlite3_finalize(${connectionPtr.ptr}, ${statementPtr.ptr}) failed: ${result.info}" }
        }
    }

    private fun executeNonQuery(
        db: WasmSqlite3ConnectionPtr,
        statement: WasmSqlite3StatementPtr,
        isPragmaStmt: Boolean,
    ) {
        var err = cApi.statement.sqlite3Step(statement.ptr)
        if (isPragmaStmt) {
            while (err == SQLITE_ROW) {
                err = cApi.statement.sqlite3Step(statement.ptr)
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
        database: WasmSqlite3ConnectionPtr,
        statement: WasmSqlite3StatementPtr,
    ) {
        val err = cApi.statement.sqlite3Step(statement.ptr)
        if (err != SQLITE_ROW) {
            throwAndroidSqliteException(database.ptr)
        }
    }

    private fun sqliteTraceCallback(trace: SqliteTrace) {
        when (trace) {
            is SqliteTrace.TraceStmt -> logger.d { """${trace.db}: "${trace.unexpandedSql}"""" }
            is SqliteTrace.TraceClose -> logger.d { """${trace.db} closed""" }
            is SqliteTrace.TraceProfile -> logger.d {
                val sql = cApi.statement.sqlite3ExpandedSql(trace.statement) ?: trace.statement.toString()
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
        val errInfo = cApi.errors.readSqliteErrorInfo(db)
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
                val type = cApi.statement.sqlite3ColumnType(statement, columnNo)
                when (type) {
                    SQLITE3_TEXT -> {
                        val text = cApi.statement.sqlite3ColumnText(statement, columnNo) ?: run {
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
                        val value = cApi.statement.sqlite3ColumnInt64(statement, columnNo)
                        val putStatus = window.putField(addedRows, columnNo, IntegerField(value))
                        if (putStatus != 0) {
                            logger.v { "Failed allocating space for a long in column $columnNo, error=$putStatus" }
                            result = CPR_FULL
                            break
                        }
                        logger.v { "${startPos + addedRows},$columnNo is INTEGER $value" }
                    }

                    SQLITE_FLOAT -> {
                        val value = cApi.statement.sqlite3ColumnDouble(statement, columnNo)
                        val putStatus = window.putField(addedRows, columnNo, FloatField(value))
                        if (putStatus != 0) {
                            logger.v { "Failed allocating space for a double in column $columnNo, error=$putStatus" }
                            result = CPR_FULL
                            break
                        }
                        logger.v { "${startPos + addedRows},$columnNo is FLOAT $value" }
                    }

                    SQLITE_BLOB -> {
                        val value = cApi.statement.sqlite3ColumnBlob(statement, columnNo)
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

    private fun <R : Any> Sqlite3Result<R>.orThrow(
        msgPrefix: String?,
    ): R = when (this) {
        is Success<R> -> this.value
        is Error -> throw SqliteException(this.info, msgPrefix)
    }

    private fun SqliteResultCode.throwOnSqliteError(
        msgPrefix: String?,
        sqliteDb: WasmPtr<SqliteDb>? = null,
    ) {
        if (this != SQLITE_OK) {
            val errInfo = if (sqliteDb != null) {
                cApi.errors.readSqliteErrorInfo(sqliteDb)
            } else {
                SqliteErrorInfo(this, this, msgPrefix)
            }
            throw SqliteException(errInfo, msgPrefix)
        }
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
