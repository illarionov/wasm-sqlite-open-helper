/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.contains
import ru.pixnews.wasm.sqlite.open.helper.common.api.or
import ru.pixnews.wasm.sqlite.open.helper.exception.AndroidSqliteCantOpenDatabaseException
import ru.pixnews.wasm.sqlite.open.helper.host.ext.encodedNullTerminatedStringLength
import ru.pixnews.wasm.sqlite.open.helper.internal.OpenHelperNativeBindings.CopyRowResult.CPR_FULL
import ru.pixnews.wasm.sqlite.open.helper.internal.OpenHelperNativeBindings.CopyRowResult.CPR_OK
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow
import ru.pixnews.wasm.sqlite.open.helper.internal.ext.throwAndroidSqliteException
import ru.pixnews.wasm.sqlite.open.helper.internal.platform.yieldSleepAroundMSec
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteConfigParameter
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDbConfigParameter
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteOpenFlags
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode.Companion.name
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteStatement
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTrace
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_CLOSE
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_ROW
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode.Companion.SQLITE_TRACE_STMT
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3CApi
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3DbFunctions
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3ErrorFunctions.Companion.readSqliteErrorInfo
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3Result

internal class OpenHelperNativeBindings(
    private val cApi: Sqlite3CApi,
    rootLogger: Logger,
) {
    private val logger = rootLogger.withTag("JvmSqlOpenHelperNativeBindings")
    private val connections = Sqlite3ConnectionRegistry()

    fun nativeInit() {
        if (cApi.embedderInfo.supportMultithreading) {
            // Enable multi-threaded mode.  In this mode, SQLite is safe to use by multiple
            // threads as long as no two threads use the same database connection at the same
            // time (which we guarantee in the SQLite database wrappers).
            cApi.config.sqlite3Config(SqliteConfigParameter.SQLITE_CONFIG_MULTITHREAD, 1)
                .throwOnError("sqlite3__wasm_config_i() failed")
        }

        // Redirect SQLite log messages to the log.
        val sqliteLogger = logger.withTag("sqlite3")
        cApi.config.sqlite3SetLogger { errCode, message -> sqliteLogger.w { "$errCode: $message" } }
            .throwOnError("sqliteSetLogger() failed")

        // The soft heap limit prevents the page cache allocations from growing
        // beyond the given limit, no matter what the max page cache sizes are
        // set to. The limit does not, as of 3.5.0, affect any other allocations.
        cApi.config.sqlite3SoftHeapLimit(SQLiteGlobal.SOFT_HEAP_LIMIT)
            .throwOnError("sqlite3_soft_heap_limit64() failed")

        cApi.config.sqlite3initialize()
            .throwOnError("sqlite3_initialize failed")
    }

    @Suppress("CyclomaticComplexMethod")
    fun nativeOpen(
        path: String,
        openFlags: SqliteOpenFlags,
        enableTrace: Boolean,
        enableProfile: Boolean,
        lookasideSlotSize: Int,
        lookasideSlotCount: Int,
    ): WasmPtr<SqliteDb> {
        var db: WasmPtr<SqliteDb>? = null
        try {
            db = cApi.db.sqlite3OpenV2(
                filename = path,
                flags = openFlags,
                vfsName = null,
            ).getOrThrow("sqlite3OpenV2() failed")

            if (lookasideSlotSize > 0 && lookasideSlotCount > 0) {
                cApi.db.sqlite3DbConfig(
                    db,
                    SqliteDbConfigParameter.SQLITE_DBCONFIG_LOOKASIDE,
                    WasmPtr.SQLITE3_NULL,
                    lookasideSlotSize,
                    lookasideSlotCount,
                ).getOrThrow("sqlite3DbConfig() failed")
            }

            // Check that the database is really read/write when that is what we asked for.
            if (openFlags.contains(SqliteOpenFlags.SQLITE_OPEN_READWRITE) &&
                cApi.db.sqlite3DbReadonly(db, null) != Sqlite3DbFunctions.SqliteDbReadonlyResult.READ_WRITE
            ) {
                throw AndroidSqliteCantOpenDatabaseException("Could not open the database in read/write mode.")
            }

            // Set the default busy handler to retry automatically before returning SQLITE_BUSY.
            cApi.db.sqlite3BusyTimeout(db, BUSY_TIMEOUT_MS)
                .getOrThrow("sqlite3BusyTimeout() failed")

            // Register custom Android functions.
            cApi.db.registerAndroidFunctions(db)
                .getOrThrow("register_android_functions() failed")

            // Register wrapper object
            connections.add(db, path)

            // Enable tracing and profiling if requested.
            if (enableTrace || enableProfile) {
                var mask = SqliteTraceEventCode(0U)
                if (enableTrace) {
                    mask = mask or SQLITE_TRACE_STMT or SQLITE_TRACE_ROW or SQLITE_TRACE_CLOSE
                }
                if (enableProfile) {
                    mask = mask or SqliteTraceEventCode.SQLITE_TRACE_PROFILE
                }
                cApi.db.sqlite3Trace(db, mask, ::sqliteTraceCallback)
            }
            return db
        } catch (@Suppress("TooGenericExceptionCaught") otherException: RuntimeException) {
            closeDatabaseSilent(db)
            throw otherException
        }
    }

    private fun closeDatabaseSilent(db: WasmPtr<SqliteDb>?) {
        db?.let { database ->
            val closeError = cApi.db.sqlite3Close(database)
            if (closeError != SqliteResultCode.SQLITE_OK) {
                logger.e { "sqlite3Close() failed with error code `${closeError.name}`" }
            }
        }
    }

    fun nativeRegisterLocalizedCollators(connectionPtr: WasmPtr<SqliteDb>, newLocale: String) {
        logger.v { "nativeRegisterLocalizedCollators($connectionPtr, $newLocale)" }
        val errCode = cApi.db.registerLocalizedCollators(connectionPtr, newLocale)
        if (errCode is Sqlite3Result.Error) {
            // This can happen if sub-objects aren't closed first.  Make sure the caller knows.
            logger.i { "register_localized_collators($connectionPtr) failed: ${errCode.info}" }
            throwAndroidSqliteException(errCode.info, "Count not close db.")
        }
    }

    fun nativeClose(connectionPtr: WasmPtr<SqliteDb>) {
        connections.remove(connectionPtr)
        val errCode: SqliteResultCode = cApi.db.sqlite3Close(connectionPtr)
        if (errCode != SqliteResultCode.SQLITE_OK) {
            // This can happen if sub-objects aren't closed first.  Make sure the caller knows.
            logger.i { "sqlite3_close($connectionPtr) failed: ${errCode.name}" }
            throwAndroidSqliteException("Count not close db.", errCode)
        }
    }

    @Suppress("COMPACT_OBJECT_INITIALIZATION")
    fun nativeResetCancel(connectionPtr: WasmPtr<SqliteDb>, cancelable: Boolean) {
        val connection = connections.get(connectionPtr) ?: run {
            logger.i { "nativeResetCancel($connectionPtr}): connection not open" }
            return
        }
        connection.isCancelled = false

        val result = if (cancelable) {
            cApi.db.sqlite3ProgressHandler(connectionPtr, 4, ::sqliteProgressHandlerCallback)
        } else {
            cApi.db.sqlite3ProgressHandler(connectionPtr, 0, null)
        }
        if (result is Sqlite3Result.Error) {
            logger.i { "nativeResetCancel($connectionPtr) failed: ${result.info}" }
        }
        result.getOrThrow("Count not close db.")
    }

    @Suppress("COMPACT_OBJECT_INITIALIZATION")
    fun nativeCancel(connectionPtr: WasmPtr<SqliteDb>) {
        val connection = connections.get(connectionPtr) ?: run {
            logger.i { "nativeResetCancel($connectionPtr): connection not open" }
            return
        }
        connection.isCancelled = true
    }

    @Suppress(
        "CyclomaticComplexMethod",
        "LOCAL_VARIABLE_EARLY_DECLARATION",
        "LongMethod",
        "LoopWithTooManyJumpStatements",
        "NAME_SHADOWING",
    )
    fun nativeExecuteForCursorWindow(
        connectionPtr: WasmPtr<SqliteDb>,
        statementPtr: WasmPtr<SqliteStatement>,
        window: NativeCursorWindow,
        startPos: Int,
        requiredPos: Int,
        countAllRows: Boolean,
    ): Long {
        val status = window.clear()
        if (status != 0) {
            throwAndroidSqliteException("Failed to clear the cursor window")
        }

        val numColumns = cApi.statement.sqlite3ColumnCount(statementPtr)
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
                val err = cApi.statement.sqlite3Step(statementPtr)
                when (err) {
                    SqliteResultCode.SQLITE_DONE -> {
                        logger.v { "Processed all rows" }
                        break
                    }

                    SqliteResultCode.SQLITE_ROW -> {
                        logger.v { "Stepped statement $statementPtr to row $totalRows" }
                        retryCount = 0
                        totalRows += 1

                        // Skip the row if the window is full or we haven't reached the start position yet.
                        if (startPos >= totalRows || windowFull) {
                            continue
                        }
                        var cpr = copyRow(window, statementPtr, numColumns, startPos, addedRows)

                        if (cpr == CPR_FULL && addedRows != 0 && startPos + addedRows <= requiredPos) {
                            // We filled the window before we got to the one row that we really wanted.
                            // Clear the window and start filling it again from here.
                            window.clear()
                            window.setNumColumns(numColumns)
                            startPos += addedRows
                            addedRows = 0
                            cpr = copyRow(window, statementPtr, numColumns, startPos, addedRows)
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
                            readErrorThrowAndroidSqliteException(connectionPtr, "retrycount exceeded")
                        } else {
                            // Sleep to give the thread holding the lock a chance to finish
                            yieldSleepAroundMSec()
                            retryCount++
                        }
                    }

                    else -> readErrorThrowAndroidSqliteException(connectionPtr, "sqlite3Step() failed")
                }
            }
        } finally {
            logger.v {
                "Resetting statement $statementPtr after fetching $totalRows rows and adding $addedRows rows" +
                        "to the window in ${window.size - window.freeSpace} bytes"
            }

            cApi.statement.sqlite3Reset(statementPtr) // TODO: check error code, may be SQLITE_BUSY
        }

        // Report the total number of rows on request.
        if (startPos > totalRows) {
            logger.e { "startPos $startPos > actual rows $totalRows" }
        }

        return (startPos.toLong().shr(32)).or(totalRows.toLong())
    }

    fun nativeExecuteForLastInsertedRowId(
        connectionPtr: WasmPtr<SqliteDb>,
        statementPtr: WasmPtr<SqliteStatement>,
    ): Long {
        executeNonQuery(connectionPtr, statementPtr, false)

        if (cApi.db.sqlite3Changes(connectionPtr) <= 0) {
            return -1
        }

        return cApi.db.sqlite3LastInsertRowId(connectionPtr)
    }

    fun nativeExecuteForChangedRowCount(
        connectionPtr: WasmPtr<SqliteDb>,
        statementPtr: WasmPtr<SqliteStatement>,
    ): Int {
        executeNonQuery(connectionPtr, statementPtr, false)
        return cApi.db.sqlite3Changes(connectionPtr)
    }

    fun nativeExecuteForString(
        connectionPtr: WasmPtr<SqliteDb>,
        statementPtr: WasmPtr<SqliteStatement>,
    ): String? {
        executeOneRowQuery(connectionPtr, statementPtr)

        if (cApi.statement.sqlite3ColumnCount(statementPtr) < 1) {
            return null
        }

        return cApi.statement.sqlite3ColumnText(statementPtr, 0)
    }

    fun nativeExecuteForLong(
        connectionPtr: WasmPtr<SqliteDb>,
        statementPtr: WasmPtr<SqliteStatement>,
    ): Long {
        executeOneRowQuery(connectionPtr, statementPtr)
        if (cApi.db.sqlite3LastInsertRowId(connectionPtr) < 1) {
            return -1
        }

        return cApi.statement.sqlite3ColumnInt64(statementPtr, 0)
    }

    fun nativeExecute(
        connectionPtr: WasmPtr<SqliteDb>,
        statementPtr: WasmPtr<SqliteStatement>,
        isPragmaStmt: Boolean,
    ) {
        executeNonQuery(connectionPtr, statementPtr, isPragmaStmt)
    }

    fun nativeResetStatement(
        connectionPtr: WasmPtr<SqliteDb>,
        statementPtr: WasmPtr<SqliteStatement>,
    ) {
        val err = cApi.statement.sqlite3Reset(statementPtr)
        if (err != SqliteResultCode.SQLITE_OK) {
            readErrorThrowAndroidSqliteException(connectionPtr)
        }
    }

    fun nativeClearBindings(
        connectionPtr: WasmPtr<SqliteDb>,
        statementPtr: WasmPtr<SqliteStatement>,
    ) {
        if (cApi.statement.sqlite3ClearBindings(statementPtr) != SqliteResultCode.SQLITE_OK) {
            readErrorThrowAndroidSqliteException(connectionPtr)
        }
    }

    fun nativeBindBlob(
        connectionPtr: WasmPtr<SqliteDb>,
        statementPtr: WasmPtr<SqliteStatement>,
        index: Int,
        value: ByteArray,
    ) {
        val err = cApi.statement.sqlite3BindBlobTransient(statementPtr, index, value)
        if (err != SqliteResultCode.SQLITE_OK) {
            readErrorThrowAndroidSqliteException(connectionPtr)
        }
    }

    fun nativeBindString(
        connectionPtr: WasmPtr<SqliteDb>,
        statementPtr: WasmPtr<SqliteStatement>,
        index: Int,
        value: String,
    ) {
        val err = cApi.statement.sqlite3BindStringTransient(statementPtr, index, value)
        if (err != SqliteResultCode.SQLITE_OK) {
            readErrorThrowAndroidSqliteException(connectionPtr)
        }
    }

    fun nativeBindDouble(
        connectionPtr: WasmPtr<SqliteDb>,
        statementPtr: WasmPtr<SqliteStatement>,
        index: Int,
        value: Double,
    ) {
        val err = cApi.statement.sqlite3BindDouble(statementPtr, index, value)
        if (err != SqliteResultCode.SQLITE_OK) {
            readErrorThrowAndroidSqliteException(connectionPtr)
        }
    }

    fun nativeBindLong(
        connectionPtr: WasmPtr<SqliteDb>,
        statementPtr: WasmPtr<SqliteStatement>,
        index: Int,
        value: Long,
    ) {
        val err = cApi.statement.sqlite3BindLong(statementPtr, index, value)
        if (err != SqliteResultCode.SQLITE_OK) {
            readErrorThrowAndroidSqliteException(connectionPtr)
        }
    }

    fun nativeBindNull(
        connectionPtr: WasmPtr<SqliteDb>,
        statementPtr: WasmPtr<SqliteStatement>,
        index: Int,
    ) {
        val err = cApi.statement.sqlite3BindNull(statementPtr, index)
        if (err != SqliteResultCode.SQLITE_OK) {
            readErrorThrowAndroidSqliteException(connectionPtr)
        }
    }

    fun nativeGetColumnName(
        statementPtr: WasmPtr<SqliteStatement>,
        index: Int,
    ): String? {
        return cApi.statement.sqlite3ColumnName(statementPtr, index)
    }

    fun nativeGetColumnCount(
        statementPtr: WasmPtr<SqliteStatement>,
    ): Int {
        return cApi.statement.sqlite3ColumnCount(statementPtr)
    }

    fun nativeIsReadOnly(
        statementPtr: WasmPtr<SqliteStatement>,
    ): Boolean {
        return cApi.statement.sqlite3StmtReadonly(statementPtr)
    }

    fun nativeGetParameterCount(
        statementPtr: WasmPtr<SqliteStatement>,
    ): Int {
        return cApi.statement.sqlite3BindParameterCount(statementPtr)
    }

    fun nativePrepareStatement(
        connectionPtr: WasmPtr<SqliteDb>,
        sql: String,
    ): WasmPtr<SqliteStatement> {
        when (val statementPtr = cApi.statement.sqlite3PrepareV2(connectionPtr, sql)) {
            is Sqlite3Result.Success -> {
                logger.v { "Prepared statement ${statementPtr.value}. on connection $connectionPtr" }
                return statementPtr.value
            }

            is Sqlite3Result.Error -> readErrorThrowAndroidSqliteException(connectionPtr, ", while compiling: $sql")
        }
    }

    fun nativeFinalizeStatement(
        connectionPtr: WasmPtr<SqliteDb>,
        statementPtr: WasmPtr<SqliteStatement>,
    ) {
        logger.v { "Finalized statement $statementPtr on connection $connectionPtr" }
        // We ignore the result of sqlite3_finalize because it is really telling us about
        // whether any errors occurred while executing the statement.  The statement itself
        // is always finalized regardless.
        val result = cApi.statement.sqlite3Finalize(connectionPtr, statementPtr)
        if (result is Sqlite3Result.Error) {
            logger.v { "sqlite3_finalize($connectionPtr, $statementPtr) failed: ${result.info}" }
        }
    }

    private fun executeNonQuery(
        db: WasmPtr<SqliteDb>,
        statement: WasmPtr<SqliteStatement>,
        isPragmaStmt: Boolean,
    ) {
        var err = cApi.statement.sqlite3Step(statement)
        if (isPragmaStmt) {
            while (err == SqliteResultCode.SQLITE_ROW) {
                err = cApi.statement.sqlite3Step(statement)
            }
        }
        when (err) {
            SqliteResultCode.SQLITE_ROW -> throwAndroidSqliteException(
                "Queries can be performed using SQLiteDatabase query or rawQuery methods only.",
            )

            SqliteResultCode.SQLITE_DONE -> Unit
            else -> readErrorThrowAndroidSqliteException(db)
        }
    }

    private fun executeOneRowQuery(
        database: WasmPtr<SqliteDb>,
        statement: WasmPtr<SqliteStatement>,
    ) {
        val err = cApi.statement.sqlite3Step(statement)
        if (err != SqliteResultCode.SQLITE_ROW) {
            readErrorThrowAndroidSqliteException(database)
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

    @Suppress("LongMethod", "CyclomaticComplexMethod")
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
                    SqliteColumnType.SQLITE3_TEXT -> {
                        val text = cApi.statement.sqlite3ColumnText(statement, columnNo) ?: run {
                            throwAndroidSqliteException("Null text at ${startPos + addedRows},$columnNo")
                        }
                        val putStatus = window.putField(addedRows, columnNo, NativeCursorWindow.Field.StringField(text))
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

                    SqliteColumnType.SQLITE_INTEGER -> {
                        val value = cApi.statement.sqlite3ColumnInt64(statement, columnNo)
                        val putStatus = window.putField(
                            addedRows,
                            columnNo,
                            NativeCursorWindow.Field.IntegerField(value),
                        )
                        if (putStatus != 0) {
                            logger.v { "Failed allocating space for a long in column $columnNo, error=$putStatus" }
                            result = CPR_FULL
                            break
                        }
                        logger.v { "${startPos + addedRows},$columnNo is INTEGER $value" }
                    }

                    SqliteColumnType.SQLITE_FLOAT -> {
                        val value = cApi.statement.sqlite3ColumnDouble(statement, columnNo)
                        val putStatus = window.putField(addedRows, columnNo, NativeCursorWindow.Field.FloatField(value))
                        if (putStatus != 0) {
                            logger.v { "Failed allocating space for a double in column $columnNo, error=$putStatus" }
                            result = CPR_FULL
                            break
                        }
                        logger.v { "${startPos + addedRows},$columnNo is FLOAT $value" }
                    }

                    SqliteColumnType.SQLITE_BLOB -> {
                        val value = cApi.statement.sqlite3ColumnBlob(statement, columnNo)
                        val putStatus = window.putField(addedRows, columnNo, NativeCursorWindow.Field.BlobField(value))
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

                    SqliteColumnType.SQLITE_NULL -> {
                        val putStatus = window.putField(addedRows, columnNo, NativeCursorWindow.Field.Null)
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

    private fun <R : Any> Sqlite3Result<R>.getOrThrow(
        msgPrefix: String?,
    ): R = when (this) {
        is Sqlite3Result.Success<R> -> this.value
        is Sqlite3Result.Error -> throwAndroidSqliteException(this.info, msgPrefix)
    }

    private fun SqliteResultCode.throwOnError(
        msgPrefix: String?,
    ) {
        if (this != SqliteResultCode.SQLITE_OK) {
            throwAndroidSqliteException(msgPrefix, this)
        }
    }

    private fun readErrorThrowAndroidSqliteException(
        db: WasmPtr<SqliteDb>,
        message: String? = null,
    ): Nothing {
        val errInfo = cApi.errors.readSqliteErrorInfo(db)
        throwAndroidSqliteException(errInfo, message)
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
