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
) {
    fun nativeCreate(name: String, cursorWindowSize: Int): NativeCursorWindow {
        return NativeCursorWindow(name, cursorWindowSize, rootLogger = rootLogger)
    }

    fun nativeGetName(windowPtr: NativeCursorWindow): String {
        return windowPtr.name
    }

    fun nativePutNull(windowPtr: NativeCursorWindow, row: Int, column: Int): Boolean {
        return windowPtr.putNull(row, column) == 0
    }

    fun nativePutDouble(windowPtr: NativeCursorWindow, value: Double, row: Int, column: Int): Boolean {
        return windowPtr.putDouble(row, column, value) == 0
    }

    fun nativePutLong(windowPtr: NativeCursorWindow, value: Long, row: Int, column: Int): Boolean {
        return windowPtr.putLong(row, column, value) == 0
    }

    fun nativePutString(windowPtr: NativeCursorWindow, value: String, row: Int, column: Int): Boolean {
        return windowPtr.putString(row, column, value) == 0
    }

    fun nativePutBlob(windowPtr: NativeCursorWindow, value: ByteArray, row: Int, column: Int): Boolean {
        return windowPtr.putBlob(row, column, value) == 0
    }

    fun nativeGetDouble(windowPtr: NativeCursorWindow, row: Int, column: Int): Double {
        val slot: FieldSlot = windowPtr.getFieldSlot(row, column) ?: error("Couldn't read row $row column $column")
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

    fun nativeGetLong(windowPtr: NativeCursorWindow, row: Int, column: Int): Long {
        val slot: FieldSlot = windowPtr.getFieldSlot(row, column) ?: error("Couldn't read row $row column $column")
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

    fun nativeGetString(windowPtr: NativeCursorWindow, row: Int, column: Int): String? {
        val slot: FieldSlot = windowPtr.getFieldSlot(row, column) ?: error("Couldn't read row $row column $column")
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

    fun nativeGetBlob(windowPtr: NativeCursorWindow, row: Int, column: Int): ByteArray? {
        val slot: FieldSlot = windowPtr.getFieldSlot(row, column) ?: error("Couldn't read row $row column $column")
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

    fun nativeGetType(windowPtr: NativeCursorWindow, row: Int, column: Int): CursorFieldType {
        val slot: FieldSlot = windowPtr.getFieldSlot(row, column) ?: error("Couldn't read row $row column $column")
        return slot.type
    }

    fun nativeFreeLastRow(windowPtr: NativeCursorWindow) {
        windowPtr.freeLastRow()
    }

    fun nativeAllocRow(windowPtr: NativeCursorWindow): Boolean {
        return windowPtr.allocRow() == 0
    }

    fun nativeSetNumColumns(windowPtr: NativeCursorWindow, columnNum: Int): Boolean {
        return windowPtr.setNumColumns(columnNum) == 0
    }

    fun nativeGetNumRows(windowPtr: NativeCursorWindow): Int {
        return windowPtr.numRows
    }

    fun nativeClear(windowPtr: NativeCursorWindow) {
        windowPtr.clear()
    }

    fun nativeDispose(windowPtr: NativeCursorWindow) {
        windowPtr.clear()
    }
}
