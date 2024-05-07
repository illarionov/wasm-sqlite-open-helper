/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.interop.capi

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.plus
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.EmbedderMemory
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readNullableNullTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.write
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.sqliteFreeSilent
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.SqliteDatabaseResourcesRegistry
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.capi.Sqlite3ErrorFunctions.Companion.createSqlite3Result
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteColumnType
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDestructorType
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteStatement

internal class Sqlite3StatementFunctions(
    private val sqliteBindings: SqliteBindings,
    private val memory: EmbedderMemory,
    private val databaseResourcesRegistry: SqliteDatabaseResourcesRegistry,
    private val sqliteErrorApi: Sqlite3ErrorFunctions,
) {
    private val memoryBindings = sqliteBindings.memoryBindings

    fun sqlite3PrepareV2(
        sqliteDb: WasmPtr<SqliteDb>,
        sql: String,
    ): Sqlite3Result<WasmPtr<SqliteStatement>> {
        var sqlBytesPtr: WasmPtr<Byte> = WasmPtr.sqlite3Null()
        var ppStatement: WasmPtr<WasmPtr<SqliteStatement>> = WasmPtr.sqlite3Null()

        try {
            val sqlEncoded = sql.encodeToByteArray()
            val nullTerminatedSqlSize = sqlEncoded.size + 1

            sqlBytesPtr = memoryBindings.sqliteAllocOrThrow(nullTerminatedSqlSize.toUInt())
            ppStatement = memoryBindings.sqliteAllocOrThrow(WasmPtr.WASM_SIZEOF_PTR)

            memory.write(sqlBytesPtr, sqlEncoded)
            memory.writeByte(sqlBytesPtr + sqlEncoded.size, 0)

            val errCode = sqliteBindings.sqlite3_prepare_v2.executeForSqliteResultCode(
                sqliteDb.addr,
                sqlBytesPtr.addr,
                nullTerminatedSqlSize,
                ppStatement.addr,
                WasmPtr.sqlite3Null<Unit>().addr,
            )
            val result = sqliteErrorApi.createSqlite3Result(
                errCode,
                memory.readPtr(ppStatement),
                sqliteDb,
            )
            if (result is Sqlite3Result.Success) {
                databaseResourcesRegistry.registerStatement(sqliteDb, result.value)
            }
            return result
        } finally {
            memoryBindings.sqliteFreeSilent(sqlBytesPtr)
            memoryBindings.sqliteFreeSilent(ppStatement)
        }
    }

    fun sqlite3ColumnCount(
        statement: WasmPtr<SqliteStatement>,
    ): Int {
        return sqliteBindings.sqlite3_column_count.executeForInt(statement.addr)
    }

    fun sqlite3ColumnType(
        statement: WasmPtr<SqliteStatement>,
        columnNo: Int,
    ): SqliteColumnType {
        return SqliteColumnType(sqliteBindings.sqlite3_column_type.executeForInt(statement.addr, columnNo))
    }

    fun sqlite3Step(
        statement: WasmPtr<SqliteStatement>,
    ): SqliteResultCode {
        return sqliteBindings.sqlite3_step.executeForSqliteResultCode(statement.addr)
    }

    fun sqlite3Reset(
        statement: WasmPtr<SqliteStatement>,
    ): SqliteResultCode {
        return sqliteBindings.sqlite3_reset.executeForSqliteResultCode(statement.addr)
    }

    fun sqlite3ClearBindings(statement: WasmPtr<SqliteStatement>): SqliteResultCode {
        return sqliteBindings.sqlite3_clear_bindings.executeForSqliteResultCode(statement.addr)
    }

    fun sqlite3BindBlobTransient(
        statement: WasmPtr<SqliteStatement>,
        index: Int,
        value: ByteArray,
    ): SqliteResultCode {
        val pValue: WasmPtr<Byte> = memoryBindings.sqliteAllocOrThrow(value.size.toUInt())
        memory.write(pValue, value, 0, value.size)
        val errCode = try {
            sqliteBindings.sqlite3_bind_blob.executeForSqliteResultCode(
                statement.addr,
                index,
                pValue.addr,
                value.size,
                SqliteDestructorType.SQLITE_TRANSIENT.id, // TODO: change to destructor?
            )
        } finally {
            memoryBindings.sqliteFreeSilent(pValue)
        }

        return errCode
    }

    fun sqlite3BindStringTransient(
        statement: WasmPtr<SqliteStatement>,
        index: Int,
        value: String,
    ): SqliteResultCode {
        val encoded = value.encodeToByteArray()
        val size = encoded.size

        val pValue: WasmPtr<Byte> = memoryBindings.sqliteAllocOrThrow(size.toUInt())
        memory.write(pValue, encoded, 0, size)
        val errCode = try {
            sqliteBindings.sqlite3_bind_text.executeForInt(
                statement.addr,
                index,
                pValue.addr,
                size,
                SqliteDestructorType.SQLITE_TRANSIENT.id, // TODO: change to destructor?
            )
        } finally {
            memoryBindings.sqliteFreeSilent(pValue)
        }

        return SqliteResultCode(errCode)
    }

    fun sqlite3BindDouble(
        statement: WasmPtr<SqliteStatement>,
        index: Int,
        value: Double,
    ): SqliteResultCode {
        return sqliteBindings.sqlite3_bind_double.executeForSqliteResultCode(
            statement.addr,
            index,
            value,
        )
    }

    fun sqlite3BindLong(
        statement: WasmPtr<SqliteStatement>,
        index: Int,
        value: Long,
    ): SqliteResultCode {
        return sqliteBindings.sqlite3_bind_int64.executeForSqliteResultCode(
            statement.addr,
            index,
            value,
        )
    }

    fun sqlite3BindNull(
        sqliteDb: WasmPtr<SqliteStatement>,
        index: Int,
    ): SqliteResultCode {
        return sqliteBindings.sqlite3_bind_null.executeForSqliteResultCode(sqliteDb.addr, index)
    }

    fun sqlite3ColumnName(
        statement: WasmPtr<SqliteStatement>,
        index: Int,
    ): String? {
        val ptr: WasmPtr<Byte> = sqliteBindings.sqlite3_column_name.executeForPtr(statement.addr, index)
        return memory.readNullableNullTerminatedString(ptr)
    }

    fun sqlite3StmtReadonly(statement: WasmPtr<SqliteStatement>): Boolean {
        return sqliteBindings.sqlite3_stmt_readonly.executeForInt(statement.addr) != 0
    }

    fun sqlite3BindParameterCount(statement: WasmPtr<SqliteStatement>): Int {
        return sqliteBindings.sqlite3_bind_parameter_count.executeForInt(statement.addr)
    }

    fun sqlite3Finalize(
        sqliteDatabase: WasmPtr<SqliteDb>,
        statement: WasmPtr<SqliteStatement>,
    ): Sqlite3Result<Unit> {
        try {
            val errCode = sqliteBindings.sqlite3_finalize.executeForSqliteResultCode(statement.addr)
            return sqliteErrorApi.createSqlite3Result(errCode, Unit, sqliteDatabase)
        } finally {
            databaseResourcesRegistry.unregisterStatement(sqliteDatabase, statement)
        }
    }

    fun sqlite3ExpandedSql(statement: WasmPtr<SqliteStatement>): String? {
        val ptr: WasmPtr<Byte> = sqliteBindings.sqlite3_expanded_sql.executeForPtr(statement.addr)
        return memory.readNullableNullTerminatedString(ptr)
    }

    fun sqlite3ColumnText(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): String? {
        val ptr: WasmPtr<Byte> = sqliteBindings.sqlite3_column_text.executeForPtr(statement.addr, columnIndex)
        return memory.readNullableNullTerminatedString(ptr)
    }

    fun sqlite3ColumnInt64(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): Long {
        return sqliteBindings.sqlite3_column_int64.executeForLong(statement.addr, columnIndex)
    }

    fun sqlite3ColumnDouble(
        statement: WasmPtr<SqliteStatement>,
        columnIndex: Int,
    ): Double {
        return sqliteBindings.sqlite3_column_double.executeForDouble(statement.addr, columnIndex)
    }

    fun sqlite3ColumnBlob(
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
}
