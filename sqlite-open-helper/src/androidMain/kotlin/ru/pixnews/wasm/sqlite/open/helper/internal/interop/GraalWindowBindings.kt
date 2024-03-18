/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.interop

import android.database.sqlite.SQLiteException
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.NativeCursorWindow.CursorFieldType
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.NativeCursorWindow.Field.BlobField
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.NativeCursorWindow.Field.FloatField
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.NativeCursorWindow.Field.IntegerField
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.NativeCursorWindow.Field.Null
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.NativeCursorWindow.Field.StringField
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.NativeCursorWindow.FieldSlot

internal class GraalWindowBindings(
    val rootLogger: Logger,
) : SqlOpenHelperWindowBindings<GraalSqlite3WindowPtr> {
    override fun nativeCreate(name: String, cursorWindowSize: Int): GraalSqlite3WindowPtr {
        return GraalSqlite3WindowPtr(NativeCursorWindow(name, cursorWindowSize, rootLogger = rootLogger))
    }

    override fun nativeGetName(windowPtr: GraalSqlite3WindowPtr): String {
        return windowPtr.ptr.name
    }

    override fun nativePutNull(windowPtr: GraalSqlite3WindowPtr, row: Int, column: Int): Boolean {
        return windowPtr.ptr.putNull(row, column) == 0
    }

    override fun nativePutDouble(windowPtr: GraalSqlite3WindowPtr, value: Double, row: Int, column: Int): Boolean {
        return windowPtr.ptr.putDouble(row, column, value) == 0
    }

    override fun nativePutLong(windowPtr: GraalSqlite3WindowPtr, value: Long, row: Int, column: Int): Boolean {
        return windowPtr.ptr.putLong(row, column, value) == 0
    }

    override fun nativePutString(windowPtr: GraalSqlite3WindowPtr, value: String, row: Int, column: Int): Boolean {
        return windowPtr.ptr.putString(row, column, value) == 0
    }

    override fun nativePutBlob(windowPtr: GraalSqlite3WindowPtr, value: ByteArray, row: Int, column: Int): Boolean {
        return windowPtr.ptr.putBlob(row, column, value) == 0
    }

    override fun nativeGetDouble(windowPtr: GraalSqlite3WindowPtr, row: Int, column: Int): Double {
        val slot: FieldSlot = windowPtr.ptr.getFieldSlot(row, column) ?: error("Couldn't read row $row column $column")
        return slot.field.let { field ->
            when (field) {
                is BlobField -> throw SQLiteException("Unable to convert BLOB to double")
                is FloatField -> field.value
                is IntegerField -> field.value.toDouble()
                Null -> 0.0
                is StringField -> field.value.toDoubleOrNull() ?: 0.0
            }
        }
    }

    override fun nativeGetLong(windowPtr: GraalSqlite3WindowPtr, row: Int, column: Int): Long {
        val slot: FieldSlot = windowPtr.ptr.getFieldSlot(row, column) ?: error("Couldn't read row $row column $column")
        return slot.field.let { field ->
            when (field) {
                is BlobField -> throw SQLiteException("Unable to convert BLOB to double")
                is FloatField -> field.value.toLong()
                is IntegerField -> field.value
                Null -> 0L
                is StringField -> field.value.toLongOrNull() ?: 0L
            }
        }
    }

    override fun nativeGetString(windowPtr: GraalSqlite3WindowPtr, row: Int, column: Int): String? {
        val slot: FieldSlot = windowPtr.ptr.getFieldSlot(row, column) ?: error("Couldn't read row $row column $column")
        return slot.field.let { field ->
            when (field) {
                is BlobField -> throw SQLiteException("Unable to convert BLOB to double")
                is FloatField -> field.value.toString()
                is IntegerField -> field.value.toString()
                Null -> null
                is StringField -> field.value
            }
        }
    }

    override fun nativeGetBlob(windowPtr: GraalSqlite3WindowPtr, row: Int, column: Int): ByteArray? {
        val slot: FieldSlot = windowPtr.ptr.getFieldSlot(row, column) ?: error("Couldn't read row $row column $column")
        return slot.field.let { field ->
            when (field) {
                is BlobField -> field.value
                is FloatField -> throw SQLiteException("FLOAT data in nativeGetBlob")
                is IntegerField -> throw SQLiteException("INTEGER data in nativeGetBlob")
                Null -> null
                is StringField -> field.value.encodeToByteArray()
            }
        }
    }

    override fun nativeGetType(windowPtr: GraalSqlite3WindowPtr, row: Int, column: Int): CursorFieldType {
        val slot: FieldSlot = windowPtr.ptr.getFieldSlot(row, column) ?: error("Couldn't read row $row column $column")
        return slot.type
    }

    override fun nativeFreeLastRow(windowPtr: GraalSqlite3WindowPtr) {
        windowPtr.ptr.freeLastRow()
    }

    override fun nativeAllocRow(windowPtr: GraalSqlite3WindowPtr): Boolean {
        return windowPtr.ptr.allocRow() == 0
    }

    override fun nativeSetNumColumns(windowPtr: GraalSqlite3WindowPtr, columnNum: Int): Boolean {
        return windowPtr.ptr.setNumColumns(columnNum) == 0
    }

    override fun nativeGetNumRows(windowPtr: GraalSqlite3WindowPtr): Int {
        return windowPtr.ptr.numRows
    }

    override fun nativeClear(windowPtr: GraalSqlite3WindowPtr) {
        windowPtr.ptr.clear()
    }

    override fun nativeDispose(windowPtr: GraalSqlite3WindowPtr) {
        windowPtr.ptr.clear()
    }
}
