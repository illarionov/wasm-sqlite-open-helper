/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteComparatorCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDbStatusParameter
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrno
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrorInfo
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteException
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteOpenFlags
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteStatement
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode

public interface SqliteCapi {
    @Throws(SqliteException::class)
    public fun sqlite3OpenV2(
        filename: String,
        flags: SqliteOpenFlags,
        vfsName: String?,
    ): WasmPtr<SqliteDb>

    @Throws(SqliteException::class)
    public fun sqlite3Close(
        sqliteDb: WasmPtr<SqliteDb>,
    )

    @Throws(SqliteException::class)
    public fun sqlite3CreateCollation(
        database: WasmPtr<SqliteDb>,
        name: String,
        comparator: SqliteComparatorCallback?,
    )

    @Throws(SqliteException::class)
    public fun sqlite3DbReadonly(
        sqliteDb: WasmPtr<SqliteDb>,
        dbName: String?,
    ): SqliteDbReadonlyResult

    @Throws(SqliteException::class)
    public fun sqlite3DbStatus(
        sqliteDb: WasmPtr<SqliteDb>,
        op: SqliteDbStatusParameter,
        resetFlag: Boolean,
    ): SqliteDbStatusResult

    @Throws(SqliteException::class)
    public fun sqlite3BusyTimeout(
        sqliteDb: WasmPtr<SqliteDb>,
        ms: Int,
    )

    @Throws(SqliteException::class)
    public fun sqlite3Trace(
        sqliteDb: WasmPtr<SqliteDb>,
        mask: SqliteTraceEventCode,
        traceCallback: SqliteTraceCallback?,
    )

    @Throws(SqliteException::class)
    public fun sqlite3ProgressHandler(
        sqliteDb: WasmPtr<SqliteDb>,
        instructions: Int,
        progressCallback: SqliteProgressCallback?,
    )

    @Throws(SqliteException::class)
    public fun sqlite3ColumnCount(
        statement: WasmPtr<SqliteStatement>,
    ): Int

    @Throws(SqliteException::class)
    public fun sqlite3Step(
        statement: WasmPtr<SqliteStatement>,
    ): SqliteErrno

    @Throws(SqliteException::class)
    public fun sqlite3Reset(
        statement: WasmPtr<SqliteStatement>,
    ): SqliteErrno

    @Throws(SqliteException::class)
    public fun sqlite3Changes(
        sqliteDb: WasmPtr<SqliteDb>,
    ): Int

    @Throws(SqliteException::class)
    public fun sqlite3LastInsertRowId(
        sqliteDb: WasmPtr<SqliteDb>,
    ): Long

    @Throws(SqliteException::class)
    public fun sqlite3ColumnText(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): String?

    @Throws(SqliteException::class)
    public fun sqlite3ColumnInt64(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): Long

    @Throws(SqliteException::class)
    public fun sqlite3ClearBindings(
        statement: WasmPtr<SqliteStatement>,
    ): SqliteErrno

    @Throws(SqliteException::class)
    public fun sqlite3BindBlobTransient(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
        value: ByteArray,
    ): SqliteErrno

    @Throws(SqliteException::class)
    public fun sqlite3BindStringTransient(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
        value: String,
    ): SqliteErrno

    @Throws(SqliteException::class)
    public fun sqlite3BindDouble(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
        value: Double,
    ): SqliteErrno

    @Throws(SqliteException::class)
    public fun sqlite3BindLong(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
        value: Long,
    ): SqliteErrno

    @Throws(SqliteException::class)
    public fun sqlite3BindNull(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
    ): SqliteErrno

    @Throws(SqliteException::class)
    public fun sqlite3ColumnName(
        statement: WasmPtr<SqliteStatement>,
        index: Int,
    ): String?

    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    @Throws(SqliteException::class)
    public fun sqlite3StmtReadonly(
        statement: WasmPtr<SqliteStatement>,
    ): Boolean

    @Throws(SqliteException::class)
    public fun sqlite3BindParameterCount(
        statement: WasmPtr<SqliteStatement>,
    ): Int

    @Throws(SqliteException::class)
    public fun sqlite3PrepareV2(
        sqliteDb: WasmPtr<SqliteDb>,
        sql: String,
    ): WasmPtr<SqliteStatement>

    @Throws(SqliteException::class)
    public fun sqlite3Finalize(
        sqliteDatabase: WasmPtr<SqliteDb>,
        statement: WasmPtr<SqliteStatement>,
    )

    @Throws(SqliteException::class)
    public fun sqlite3ExpandedSql(
        statement: WasmPtr<SqliteStatement>,
    ): String?

    @Throws(SqliteException::class)
    public fun readSqliteErrorInfo(
        sqliteDb: WasmPtr<SqliteDb>,
    ): SqliteErrorInfo

    @Throws(SqliteException::class)
    public fun sqlite3ColumnType(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): SqliteColumnType

    @Throws(SqliteException::class)
    public fun sqlite3ColumnDouble(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): Double

    @Throws(SqliteException::class)
    public fun sqlite3ColumnBlob(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): ByteArray

    @Throws(SqliteException::class)
    public fun nativeRegisterLocalizedCollators(ptr: WasmPtr<SqliteDb>, newLocale: String, utf16Storage: Boolean)

    @Throws(SqliteException::class)
    public fun registerAndroidFunctions(db: WasmPtr<SqliteDb>, utf16Storage: Boolean)

    public enum class SqliteDbReadonlyResult(public val id: Int) {
        READ_ONLY(1),
        READ_WRITE(0),
        INVALID_NAME(-1),

        ;

        public companion object {
            public fun fromId(id: Int): SqliteDbReadonlyResult = entries.first { it.id == id }
        }
    }

    public class SqliteDbStatusResult(
        public val current: Int,
        public val highestInstantaneousValue: Int,
    )
}
