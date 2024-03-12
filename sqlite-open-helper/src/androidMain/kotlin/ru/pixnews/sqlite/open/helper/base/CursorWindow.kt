/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.base

/*
 * Original Copyrights:
 * Copyright (C) 2017-2024 requery.io
 * Copyright (C) 2005-2012 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

import android.database.CharArrayBuffer
import ru.pixnews.sqlite.open.helper.internal.SQLiteClosable
import ru.pixnews.sqlite.open.helper.internal.interop.NativeCursorWindow
import ru.pixnews.sqlite.open.helper.internal.interop.SqlOpenHelperWindowBindings
import ru.pixnews.sqlite.open.helper.internal.interop.Sqlite3WindowPtr

/**
 * A buffer containing multiple cursor rows.
 */
internal class CursorWindow<WP : Sqlite3WindowPtr>(
    name: String?,
    private val bindings: SqlOpenHelperWindowBindings<WP>,
    val windowSizeBytes: Int = WINDOW_SIZE_KB * @Suppress("MagicNumber") 1024,
) : SQLiteClosable() {
    /**
     * The native CursorWindow object pointer.  (FOR INTERNAL USE ONLY)
     */
    var windowPtr: WP?

    /**
     * Sets the start position of this cursor window.
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

    val numRows: Int

        /**
         * Gets the number of rows in this window.
         *
         * @return The number of rows in this cursor window.
         */
        get() = bindings.nativeGetNumRows(windowPtr!!)

    /**
     * Creates a new empty cursor window and gives it a name.
     *
     *
     * The cursor initially has no rows or columns.  Call [.setNumColumns] to
     * set the number of columns before adding any rows to the cursor.
     *
     *
     * @param name The name of the cursor window, or null if none.
     * @param windowSizeBytes Size of cursor window in bytes.
     *
     * Note: Memory is dynamically allocated as data rows are added to
     * the window. Depending on the amount of data stored, the actual
     * amount of memory allocated can be lower than specified size,
     * but cannot exceed it. Value is a non-negative number of bytes.
     */

/**
     * Creates a new empty cursor with default cursor size (currently 2MB)
     */
    init {
        windowPtr = bindings.nativeCreate(
            this.name,
            windowSizeBytes,
        ) ?: throw CursorWindowAllocationException(
            @Suppress("MagicNumber")
            "Cursor window allocation of ${windowSizeBytes / 1024} kb failed. ",
        )
    }

    protected fun finalize() {
        dispose()
    }

    private fun dispose() {
        windowPtr?.let {
            bindings.nativeDispose(it)
            windowPtr = null
        }
    }

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
        bindings.nativeClear(windowPtr!!)
    }

    /**
     * Sets the number of columns in this window.
     *
     *
     * This method must be called before any rows are added to the window, otherwise
     * it will fail to set the number of columns if it differs from the current number
     * of columns.
     *
     *
     * @param columnNum The new number of columns.
     * @return True if successful.
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun setNumColumns(columnNum: Int): Boolean {
        return bindings.nativeSetNumColumns(windowPtr!!, columnNum)
    }

    /**
     * Allocates a new row at the end of this cursor window.
     *
     * @return True if successful, false if the cursor window is out of memory.
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun allocRow(): Boolean {
        return bindings.nativeAllocRow(windowPtr!!)
    }

    /**
     * Frees the last row in this cursor window.
     */
    fun freeLastRow() {
        bindings.nativeFreeLastRow(windowPtr!!)
    }

    /**
     * Returns the type of the field at the specified row and column index.
     *
     *
     * The returned field types are:
     *
     *  * [Cursor.FIELD_TYPE_NULL]
     *  * [Cursor.FIELD_TYPE_INTEGER]
     *  * [Cursor.FIELD_TYPE_FLOAT]
     *  * [Cursor.FIELD_TYPE_STRING]
     *  * [Cursor.FIELD_TYPE_BLOB]
     *
     *
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The field type.
     */
    fun getType(row: Int, column: Int): NativeCursorWindow.CursorFieldType {
        return bindings.nativeGetType(windowPtr!!, row - startPosition, column)
    }

    /**
     * Gets the value of the field at the specified row and column index as a byte array.
     *
     *
     * The result is determined as follows:
     *
     *  * If the field is of type [Cursor.FIELD_TYPE_NULL], then the result
     * is `null`.
     *  * If the field is of type [Cursor.FIELD_TYPE_BLOB], then the result
     * is the blob value.
     *  * If the field is of type [Cursor.FIELD_TYPE_STRING], then the result
     * is the array of bytes that make up the internal representation of the
     * string value.
     *  * If the field is of type [Cursor.FIELD_TYPE_INTEGER] or
     * [Cursor.FIELD_TYPE_FLOAT], then a [SQLiteException] is thrown.
     *
     *
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as a byte array.
     */
    fun getBlob(row: Int, column: Int): ByteArray? {
        return bindings.nativeGetBlob(windowPtr!!, row - startPosition, column)
    }

    /**
     * Gets the value of the field at the specified row and column index as a string.
     *
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
     *
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as a string.
     */
    fun getString(row: Int, column: Int): String? {
        return bindings.nativeGetString(windowPtr!!, row - startPosition, column)
    }

    /**
     * Copies the text of the field at the specified row and column index into
     * a [CharArrayBuffer].
     *
     *
     * The buffer is populated as follows:
     *
     *  * If the buffer is too small for the value to be copied, then it is
     * automatically resized.
     *  * If the field is of type [Cursor.FIELD_TYPE_NULL], then the buffer
     * is set to an empty string.
     *  * If the field is of type [Cursor.FIELD_TYPE_STRING], then the buffer
     * is set to the contents of the string.
     *  * If the field is of type [Cursor.FIELD_TYPE_INTEGER], then the buffer
     * is set to a string representation of the integer in decimal, obtained by formatting the
     * value with the `printf` family of functions using
     * format specifier `%lld`.
     *  * If the field is of type [Cursor.FIELD_TYPE_FLOAT], then the buffer is
     * set to a string representation of the floating-point value in decimal, obtained by
     * formatting the value with the `printf` family of functions using
     * format specifier `%g`.
     *  * If the field is of type [Cursor.FIELD_TYPE_BLOB], then a
     * [SQLiteException] is thrown.
     *
     *
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @param buffer The [CharArrayBuffer] to hold the string.  It is automatically
     * resized if the requested string is larger than the buffer's current capacity.
     */
    fun copyStringToBuffer(row: Int, column: Int, buffer: CharArrayBuffer?) {
        requireNotNull(buffer) { "CharArrayBuffer should not be null" }
        // TODO not as optimal as the original code
        val chars = getString(row, column)?.toCharArray() ?: charArrayOf()
        buffer.data = chars
        buffer.sizeCopied = chars.size
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
     * [SQLiteException] is thrown.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as a `long`.
     */
    fun getLong(row: Int, column: Int): Long {
        return bindings.nativeGetLong(windowPtr!!, row - startPosition, column)
    }

    /**
     * Gets the value of the field at the specified row and column index as a
     * `double`.
     *
     *
     * The result is determined as follows:
     *
     *  * If the field is of type [Cursor.FIELD_TYPE_NULL], then the result
     * is `0.0`.
     *  * If the field is of type [Cursor.FIELD_TYPE_STRING], then the result
     * is the value obtained by parsing the string value with `strtod`.
     *  * If the field is of type [Cursor.FIELD_TYPE_INTEGER], then the result
     * is the integer value converted to a `double`.
     *  * If the field is of type [Cursor.FIELD_TYPE_FLOAT], then the result
     * is the `double` value.
     *  * If the field is of type [Cursor.FIELD_TYPE_BLOB], then a
     * [SQLiteException] is thrown.
     *
     *
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as a `double`.
     */
    fun getDouble(row: Int, column: Int): Double {
        return bindings.nativeGetDouble(windowPtr!!, row - startPosition, column)
    }

    /**
     * Gets the value of the field at the specified row and column index as a
     * `short`.
     *
     *
     * The result is determined by invoking [.getLong] and converting the
     * result to `short`.
     *
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as a `short`.
     */
    fun getShort(row: Int, column: Int): Short {
        return getLong(row, column).toShort()
    }

    /**
     * Gets the value of the field at the specified row and column index as an
     * `int`.
     *
     *
     * The result is determined by invoking [.getLong] and converting the
     * result to `int`.
     *
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as an `int`.
     */
    fun getInt(row: Int, column: Int): Int {
        return getLong(row, column).toInt()
    }

    /**
     * Gets the value of the field at the specified row and column index as a
     * `float`.
     *
     *
     * The result is determined by invoking [.getDouble] and converting the
     * result to `float`.
     *
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return The value of the field as an `float`.
     */
    fun getFloat(row: Int, column: Int): Float {
        return getDouble(row, column).toFloat()
    }

    /**
     * Copies a byte array into the field at the specified row and column index.
     *
     * @param value The value to store.
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return True if successful.
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun putBlob(value: ByteArray, row: Int, column: Int): Boolean {
        return bindings.nativePutBlob(windowPtr!!, value, row - startPosition, column)
    }

    /**
     * Copies a string into the field at the specified row and column index.
     *
     * @param value The value to store.
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return True if successful.
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun putString(value: String, row: Int, column: Int): Boolean {
        return bindings.nativePutString(windowPtr!!, value, row - startPosition, column)
    }

    /**
     * Puts a long integer into the field at the specified row and column index.
     *
     * @param value The value to store.
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return True if successful.
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun putLong(value: Long, row: Int, column: Int): Boolean {
        return bindings.nativePutLong(windowPtr!!, value, row - startPosition, column)
    }

    /**
     * Puts a double-precision floating point value into the field at the
     * specified row and column index.
     *
     * @param value The value to store.
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return True if successful.
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun putDouble(value: Double, row: Int, column: Int): Boolean {
        return bindings.nativePutDouble(windowPtr!!, value, row - startPosition, column)
    }

    /**
     * Puts a null value into the field at the specified row and column index.
     *
     * @param row The zero-based row index.
     * @param column The zero-based column index.
     * @return True if successful.
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    fun putNull(row: Int, column: Int): Boolean {
        return bindings.nativePutNull(windowPtr!!, row - startPosition, column)
    }

    override fun onAllReferencesReleased() {
        dispose()
    }

    override fun toString(): String = "$name {$windowPtr}"

    companion object {
        private const val WINDOW_SIZE_KB = 2048
    }
}
