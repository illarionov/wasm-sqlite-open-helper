/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper

import ru.pixnews.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteCapi
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteCapi.SqliteDbReadonlyResult
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteCapi.SqliteDbStatusResult
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteColumnType
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteComparatorCallback
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteDbStatusParameter
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteErrno
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteErrorInfo
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteOpenFlags
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteStatement
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteTraceCallback
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode

// TODO: remove
internal class DummySqliteCapi : SqliteCapi {
    override fun sqlite3OpenV2(filename: String, flags: SqliteOpenFlags, vfsName: String?): WasmPtr<SqliteDb> {
        TODO("Not yet implemented")
    }

    override fun sqlite3Close(sqliteDb: WasmPtr<SqliteDb>) {
        TODO("Not yet implemented")
    }

    override fun sqlite3CreateCollation(
        database: WasmPtr<SqliteDb>,
        name: String,
        comparator: SqliteComparatorCallback?,
    ) {
        TODO("Not yet implemented")
    }

    override fun sqlite3DbReadonly(sqliteDb: WasmPtr<SqliteDb>, dbName: String?): SqliteDbReadonlyResult {
        TODO("Not yet implemented")
    }

    override fun sqlite3DbStatus(
        sqliteDb: WasmPtr<SqliteDb>,
        op: SqliteDbStatusParameter,
        resetFlag: Boolean,
    ): SqliteDbStatusResult {
        TODO("Not yet implemented")
    }

    override fun sqlite3BusyTimeout(sqliteDb: WasmPtr<SqliteDb>, ms: Int) {
        TODO("Not yet implemented")
    }

    override fun sqlite3Trace(
        sqliteDb: WasmPtr<SqliteDb>,
        mask: SqliteTraceEventCode,
        traceCallback: SqliteTraceCallback?,
    ) {
        TODO("Not yet implemented")
    }

    override fun sqlite3ProgressHandler(
        sqliteDb: WasmPtr<SqliteDb>,
        instructions: Int,
        progressCallback: SqliteProgressCallback?,
    ) {
        TODO("Not yet implemented")
    }

    override fun sqlite3ColumnCount(statement: WasmPtr<SqliteStatement>): Int {
        TODO("Not yet implemented")
    }

    override fun sqlite3Step(statement: WasmPtr<SqliteStatement>): SqliteErrno {
        TODO("Not yet implemented")
    }

    override fun sqlite3Reset(statement: WasmPtr<SqliteStatement>): SqliteErrno {
        TODO("Not yet implemented")
    }

    override fun sqlite3Changes(sqliteDb: WasmPtr<SqliteDb>): Int {
        TODO("Not yet implemented")
    }

    override fun sqlite3LastInsertRowId(sqliteDb: WasmPtr<SqliteDb>): Long {
        TODO("Not yet implemented")
    }

    override fun sqlite3ColumnText(statement: WasmPtr<SqliteStatement>, columnIndex: Int): String? {
        TODO("Not yet implemented")
    }

    override fun sqlite3ColumnInt64(statement: WasmPtr<SqliteStatement>, columnIndex: Int): Long {
        TODO("Not yet implemented")
    }

    override fun sqlite3ClearBindings(statement: WasmPtr<SqliteStatement>): SqliteErrno {
        TODO("Not yet implemented")
    }

    override fun sqlite3BindBlobTransient(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
        value: ByteArray,
    ): SqliteErrno {
        TODO("Not yet implemented")
    }

    override fun sqlite3BindStringTransient(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
        value: String,
    ): SqliteErrno {
        TODO("Not yet implemented")
    }

    override fun sqlite3BindDouble(sqliteDb: WasmPtr<SqliteStatement>, index: Int, value: Double): SqliteErrno {
        TODO("Not yet implemented")
    }

    override fun sqlite3BindLong(sqliteDb: WasmPtr<SqliteStatement>, index: Int, value: Long): SqliteErrno {
        TODO("Not yet implemented")
    }

    override fun sqlite3BindNull(sqliteDb: WasmPtr<SqliteStatement>, index: Int): SqliteErrno {
        TODO("Not yet implemented")
    }

    override fun sqlite3ColumnName(statement: WasmPtr<SqliteStatement>, index: Int): String? {
        TODO("Not yet implemented")
    }

    override fun sqlite3StmtReadonly(statement: WasmPtr<SqliteStatement>): Boolean {
        TODO("Not yet implemented")
    }

    override fun sqlite3BindParameterCount(statement: WasmPtr<SqliteStatement>): Int {
        TODO("Not yet implemented")
    }

    override fun sqlite3PrepareV2(sqliteDb: WasmPtr<SqliteDb>, sql: String): WasmPtr<SqliteStatement> {
        TODO("Not yet implemented")
    }

    override fun sqlite3Finalize(sqliteDatabase: WasmPtr<SqliteDb>, statement: WasmPtr<SqliteStatement>) {
        TODO("Not yet implemented")
    }

    override fun sqlite3ExpandedSql(statement: WasmPtr<SqliteStatement>): String? {
        TODO("Not yet implemented")
    }

    override fun readSqliteErrorInfo(sqliteDb: WasmPtr<SqliteDb>): SqliteErrorInfo {
        TODO("Not yet implemented")
    }

    override fun sqlite3ColumnType(statement: WasmPtr<SqliteStatement>, columnIndex: Int): SqliteColumnType {
        TODO("Not yet implemented")
    }

    override fun sqlite3ColumnDouble(statement: WasmPtr<SqliteStatement>, columnIndex: Int): Double {
        TODO("Not yet implemented")
    }

    override fun sqlite3ColumnBlob(statement: WasmPtr<SqliteStatement>, columnIndex: Int): ByteArray {
        TODO("Not yet implemented")
    }
}
