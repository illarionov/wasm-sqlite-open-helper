/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.cursor

import at.released.weh.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.exception.AndroidSqliteException
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.CursorFieldType
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.Field

/**
 *
 * A buffer containing multiple cursor rows.
 *
 * The cursor initially has no rows or columns.  Call [.setNumColumns] to
 * set the number of columns before adding any rows to the cursor.
 *
 * @param name The name of the cursor window, or null if none.
 * @param windowSizeBytes Maximum size of cursor window in bytes.
 * @throws IllegalArgumentException if {@code windowSizeBytes} is less than 0
 * @throws AssertionError if created window pointer is 0
 */
internal class CursorWindow(
    name: String?,
    rootLogger: Logger,
    private val windowSizeBytes: Int = WINDOW_SIZE_KB * 1024,
) {
    private val logger = rootLogger.withTag("CursorWindow")

    /**
     *  Sets the start position of this cursor window.
     * The start position is the zero-based index of the first row that this window contains
     * relative to the entire result set of the [Cursor].
     *
     * @param pos The new zero-based start position.
     */
    var startPosition: Int = 0

    /**
     * Gets the name of this cursor window, never null.
     */
    val name: String = if (name?.isNotEmpty() == true) name else "<unnamed>"
    var window: NativeCursorWindow = NativeCursorWindow(this.name, windowSizeBytes, logger)
        private set

    /**
     * Gets the number of rows in this window.
     *
     * @return The number of rows in this cursor window.
     */
    val numRows: Int
        get() = window.numRows

    init {
        require(windowSizeBytes >= 0) { "Window size cannot be less than 0" }
    }

    /**
     * Clears out the existing contents of the window, making it safe to reuse for new data.
     *
     * The start position ([.getStartPosition]), number of rows ([.getNumRows]),
     * and number of columns in the cursor are all reset to zero.
     *
     */
    fun clear() {
        startPosition = 0
        window = NativeCursorWindow(name, windowSizeBytes, logger)
    }

    /**
     * Returns the type of the field at the specified row and column index.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The field type.
     */
    fun getType(row: Int, column: Int): CursorFieldType = requireField(row, column).type

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
    fun getBlob(row: Int, column: Int): ByteArray? = when (val field = requireField(row, column)) {
        is Field.BlobField -> field.value
        is Field.FloatField -> throw AndroidSqliteException("FLOAT data in nativeGetBlob")
        is Field.IntegerField -> throw AndroidSqliteException("INTEGER data in nativeGetBlob")
        is Field.StringField -> field.value.encodeToByteArray()
        Field.Null -> null
    }

    /**
     * Gets the value of the field at the specified row and column index as a string.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as a string.
     * @throws AndroidSqliteException
     */
    fun getString(row: Int, column: Int): String? = when (val field = requireField(row, column)) {
        is Field.BlobField -> throw AndroidSqliteException("Unable to convert BLOB to double")
        is Field.FloatField -> field.value.toString()
        is Field.IntegerField -> field.value.toString()
        is Field.StringField -> field.value
        Field.Null -> null
    }

    /**
     * Gets the value of the field at the specified row and column index as a `long`.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as a `long`.
     * @throws AndroidSqliteException
     */
    fun getLong(row: Int, column: Int): Long = when (val field = requireField(row, column)) {
        is Field.BlobField -> throw AndroidSqliteException("Unable to convert BLOB to double")
        is Field.FloatField -> field.value.toLong()
        is Field.IntegerField -> field.value
        is Field.StringField -> field.value.toLongOrNull() ?: 0L
        Field.Null -> 0L
    }

    /**
     * Gets the value of the field at the specified row and column index as a `double`.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as a `double`.
     * @throws AndroidSqliteException
     */
    fun getDouble(row: Int, column: Int): Double = when (val field = requireField(row, column)) {
        is Field.BlobField -> throw AndroidSqliteException("Unable to convert BLOB to double")
        is Field.FloatField -> field.value
        is Field.IntegerField -> field.value.toDouble()
        is Field.StringField -> field.value.toDoubleOrNull() ?: 0.0
        Field.Null -> 0.0
    }

    /**
     * Gets the value of the field at the specified row and column index as a `short`.
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

    private fun requireField(row: Int, column: Int): Field = window.getField(row - startPosition, column) ?: error(
        "Couldn't read row $row column $column",
    )

    override fun toString(): String = "$name {$window}"

    internal companion object {
        private const val WINDOW_SIZE_KB = 2048
    }
}
