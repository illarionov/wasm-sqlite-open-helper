/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.cursor

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.exception.AndroidSqliteException
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteClosable

/**
 *
 * A buffer containing multiple cursor rows.
 *
 * The cursor initially has no rows or columns.  Call [.setNumColumns] to
 * set the number of columns before adding any rows to the cursor.
 *
 * @param name The name of the cursor window, or null if none.
 * @param windowSizeBytes Maximym size of cursor window in bytes.
 */
internal class CursorWindow(
    name: String?,
    rootLogger: Logger,
    val windowSizeBytes: Int = WINDOW_SIZE_KB * 1024,
) : SQLiteClosable() {
    /**
     *
     *  Sets the start position of this cursor window.
     *
     *
     * The start position is the zero-based index of the first row that this window contains
     * relative to the entire result set of the [Cursor].
     *
     *
     * @param pos The new zero-based start position.
     */
    var startPosition: Int = 0

    /**
     * Gets the name of this cursor window, never null.
     */
    val name: String = if (name?.isNotEmpty() == true) name else "<unnamed>"
    var window: NativeCursorWindow = NativeCursorWindow(this.name, windowSizeBytes, rootLogger = rootLogger)

    /**
     * Gets the number of rows in this window.
     *
     * @return The number of rows in this cursor window.
     */
    val numRows: Int
        get() = window.numRows

    /**
     * Clears out the existing contents of the window, making it safe to reuse
     * for new data.
     *
     *
     * The start position ([.getStartPosition]), number of rows ([.getNumRows]),
     * and number of columns in the cursor are all reset to zero.
     *
     */
    fun clear() {
        startPosition = 0
        window.clear()
    }

    /**
     * Returns the type of the field at the specified row and column index.
     *
     * The returned field types are:
     *
     *  * [Cursor.FIELD_TYPE_NULL]
     *  * [Cursor.FIELD_TYPE_INTEGER]
     *  * [Cursor.FIELD_TYPE_FLOAT]
     *  * [Cursor.FIELD_TYPE_STRING]
     *  * [Cursor.FIELD_TYPE_BLOB]
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The field type.
     */
    fun getType(row: Int, column: Int): NativeCursorWindow.CursorFieldType {
        val slot: NativeCursorWindow.FieldSlot = window.getFieldSlot(row - startPosition, column) ?: error(
            "Couldn't read row $row column $column",
        )
        return slot.type
    }

    /**
     * Gets the value of the field at the specified row and column index as a byte array.
     *
     * The result is determined as follows:
     *
     *  * If the field is of type [Cursor.FIELD_TYPE_NULL], then the result is `null`.
     *  * If the field is of type [Cursor.FIELD_TYPE_BLOB], then the resultis the blob value.
     *  * If the field is of type [Cursor.FIELD_TYPE_STRING], then the result is the array of bytes that make up the
     *  internal representation of the string value.
     *  * If the field is of type [Cursor.FIELD_TYPE_INTEGER] or [Cursor.FIELD_TYPE_FLOAT], then a
     *  [AndroidSqliteException] is thrown.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as a byte array.
     * @throws AndroidSqliteException
     */
    fun getBlob(row: Int, column: Int): ByteArray? {
        val slot: NativeCursorWindow.FieldSlot = window.getFieldSlot(row - startPosition, column) ?: error(
            "Couldn't read row $row column $column",
        )

        return slot.field.let { field ->
            when (field) {
                is NativeCursorWindow.Field.BlobField -> field.value
                is NativeCursorWindow.Field.FloatField -> throw AndroidSqliteException(
                    "FLOAT data in nativeGetBlob",
                )

                is NativeCursorWindow.Field.IntegerField -> throw AndroidSqliteException(
                    "INTEGER data in nativeGetBlob",
                )

                NativeCursorWindow.Field.Null -> null
                is NativeCursorWindow.Field.StringField -> field.value.encodeToByteArray()
            }
        }
    }

    /**
     * Gets the value of the field at the specified row and column index as a string.
     *
     * The result is determined as follows:
     *
     *  * If the field is of type [Cursor.FIELD_TYPE_NULL], then the result
     * is `null`.
     *  * If the field is of type [Cursor.FIELD_TYPE_STRING], then the result
     * is the string value.
     *  * If the field is of type [Cursor.FIELD_TYPE_INTEGER], then the result
     * is a string representation of the integer in decimal, obtained by formatting the
     * value with the `printf` family of functions using
     * format specifier `%lld`.
     *  * If the field is of type [Cursor.FIELD_TYPE_FLOAT], then the result
     * is a string representation of the floating-point value in decimal, obtained by
     * formatting the value with the `printf` family of functions using
     * format specifier `%g`.
     *  * If the field is of type [Cursor.FIELD_TYPE_BLOB], then a
     * [SQLiteException] is thrown.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as a string.
     * @throws AndroidSqliteException
     */
    fun getString(row: Int, column: Int): String? {
        val slot: NativeCursorWindow.FieldSlot = window.getFieldSlot(row - startPosition, column) ?: error(
            "Couldn't read row $row column $column",
        )

        return slot.field.let { field ->
            when (field) {
                is NativeCursorWindow.Field.BlobField -> throw AndroidSqliteException(
                    "Unable to convert BLOB to double",
                )

                is NativeCursorWindow.Field.FloatField -> field.value.toString()
                is NativeCursorWindow.Field.IntegerField -> field.value.toString()
                NativeCursorWindow.Field.Null -> null
                is NativeCursorWindow.Field.StringField -> field.value
            }
        }
    }

    /**
     * Gets the value of the field at the specified row and column index as a `long`.
     *
     *
     * The result is determined as follows:
     *
     *  * If the field is of type [Cursor.FIELD_TYPE_NULL], then the result
     * is `0L`.
     *  * If the field is of type [Cursor.FIELD_TYPE_STRING], then the result
     * is the value obtained by parsing the string value with `strtoll`.
     *  * If the field is of type [Cursor.FIELD_TYPE_INTEGER], then the result
     * is the `long` value.
     *  * If the field is of type [Cursor.FIELD_TYPE_FLOAT], then the result
     * is the floating-point value converted to a `long`.
     *  * If the field is of type [Cursor.FIELD_TYPE_BLOB], then a
     * [AndroidSqliteException] is thrown.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as a `long`.
     * @throws AndroidSqliteException
     */
    fun getLong(row: Int, column: Int): Long {
        val slot: NativeCursorWindow.FieldSlot = window.getFieldSlot(row - startPosition, column) ?: error(
            "Couldn't read row $row column $column",
        )

        return slot.field.let { field ->
            when (field) {
                is NativeCursorWindow.Field.BlobField -> throw AndroidSqliteException(
                    "Unable to convert BLOB to double",
                )

                is NativeCursorWindow.Field.FloatField -> field.value.toLong()
                is NativeCursorWindow.Field.IntegerField -> field.value
                NativeCursorWindow.Field.Null -> 0L
                is NativeCursorWindow.Field.StringField -> field.value.toLongOrNull() ?: 0L
            }
        }
    }

    /**
     * Gets the value of the field at the specified row and column index as a
     * `double`.
     *
     * The result is determined as follows:
     *
     *  * If the field is of type Cursor.FIELD_TYPE_NULL, then the result
     * is `0.0`.
     *  * If the field is of type Cursor.FIELD_TYPE_STRING, then the result
     * is the value obtained by parsing the string value with `strtod`.
     *  * If the field is of type Cursor.FIELD_TYPE_INTEGER, then the result
     * is the integer value converted to a `double`.
     *  * If the field is of type Cursor.FIELD_TYPE_FLOAT, then the result
     * is the `double` value.
     *  * If the field is of type Cursor.FIELD_TYPE_BLOB, then a
     * [AndroidSqliteException] is thrown.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as a `double`.
     * @throws AndroidSqliteException
     */
    fun getDouble(row: Int, column: Int): Double {
        val slot: NativeCursorWindow.FieldSlot = window.getFieldSlot(row - startPosition, column) ?: error(
            "Couldn't read row $row column $column",
        )
        return slot.field.let { field ->
            when (field) {
                is NativeCursorWindow.Field.BlobField -> throw AndroidSqliteException(
                    "Unable to convert BLOB to double",
                )

                is NativeCursorWindow.Field.FloatField -> field.value
                is NativeCursorWindow.Field.IntegerField -> field.value.toDouble()
                NativeCursorWindow.Field.Null -> 0.0
                is NativeCursorWindow.Field.StringField -> field.value.toDoubleOrNull() ?: 0.0
            }
        }
    }

    /**
     * Gets the value of the field at the specified row and column index as a
     * `short`.
     *
     * The result is determined by invoking [.getLong] and converting the
     * result to `short`.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as a `short`.
     */
    fun getShort(row: Int, column: Int): Short = getLong(row, column).toShort()

    /**
     * Gets the value of the field at the specified row and column index as an
     * `int`.
     *
     * The result is determined by invoking [.getLong] and converting the
     * result to `int`.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as an `int`.
     */
    fun getInt(row: Int, column: Int): Int = getLong(row, column).toInt()

    /**
     * Gets the value of the field at the specified row and column index as a
     * `float`.
     *
     * The result is determined by invoking [.getDouble] and converting the
     * result to `float`.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as an `float`.
     */
    fun getFloat(row: Int, column: Int): Float {
        return getDouble(row, column).toFloat()
    }

    override fun onAllReferencesReleased() {
        window.clear()
    }

    override fun toString(): String = "$name {$window}"

    companion object {
        private const val WINDOW_SIZE_KB = 2048
    }
}
