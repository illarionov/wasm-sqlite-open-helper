/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.base

/*
 * Original Copyrights:
 * Copyright (C) 2017-2024 requery.io
 * Copyright (C) 2005-2012 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

import android.database.CharArrayBuffer
import android.database.StaleDataException
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.CursorWindow
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow

/**
 * A base class for Cursors that store their data in [android.database.CursorWindow]s.
 *
 * The cursor owns the cursor window it uses.  When the cursor is closed,
 * its window is also closed.  Likewise, when the window used by the cursor is
 * changed, its old window is closed.  This policy of strict ownership ensures
 * that cursor windows are not leaked.
 *
 * Subclasses are responsible for filling the cursor window with data during
 * [.onMove], allocating a new cursor window if necessary.
 * During [.requery], the existing cursor window should be cleared and
 * filled with new data.
 *
 * If the contents of the cursor change or become invalid, the old window must be closed
 * (because it is owned by the cursor) and set to null.
 *
 */
internal abstract class AbstractWindowedCursor(
    private val windowFactory: (name: String?) -> CursorWindow,
) : AbstractCursor() {
    /**
     * The cursor window owned by this cursor.
     */
    open var window: CursorWindow? = null
        /**
         * Sets a new cursor window for the cursor to use.
         *
         *
         * The cursor takes ownership of the provided cursor window; the cursor window
         * will be closed when the cursor is closed or when the cursor adopts a new
         * cursor window.
         *
         *
         * If the cursor previously had a cursor window, then it is closed when the
         * new cursor window is assigned.
         *
         *
         * @param newWindow The new cursor window, typically a remote cursor window.
         */
        set(newWindow) {
            if (newWindow !== field) {
                val old = field
                field = newWindow
                old?.close()
            }
        }

    override fun getBlob(column: Int): ByteArray {
        checkPosition()
        return window!!.getBlob(pos, column) ?: byteArrayOf()
    }

    override fun getString(column: Int): String? {
        checkPosition()
        return window!!.getString(pos, column)
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
     */
    override fun copyStringToBuffer(columnIndex: Int, buffer: CharArrayBuffer) {
        val chars = getString(columnIndex)?.toCharArray() ?: charArrayOf()
        buffer.data = chars
        buffer.sizeCopied = chars.size
    }

    override fun getShort(column: Int): Short {
        checkPosition()
        return window!!.getShort(pos, column)
    }

    override fun getInt(column: Int): Int {
        checkPosition()
        return window!!.getInt(pos, column)
    }

    override fun getLong(column: Int): Long {
        checkPosition()
        return window!!.getLong(pos, column)
    }

    override fun getFloat(column: Int): Float {
        checkPosition()
        return window!!.getFloat(pos, column)
    }

    override fun getDouble(column: Int): Double {
        checkPosition()
        return window!!.getDouble(pos, column)
    }

    override fun isNull(column: Int): Boolean {
        return window!!.getType(pos, column) == NativeCursorWindow.CursorFieldType.NULL
    }

    override fun getType(column: Int): Int {
        return window!!.getType(pos, column).id
    }

    override fun checkPosition() {
        super.checkPosition()
        if (window == null) {
            throw StaleDataException(
                "Attempting to access a closed CursorWindow." +
                        "Most probable cause: cursor is deactivated prior to calling this method.",
            )
        }
    }

    /**
     * Returns true if the cursor has an associated cursor window.
     *
     * @return True if the cursor has an associated cursor window.
     */
    fun hasWindow(): Boolean = window != null

    /**
     * If there is a window, clear it. Otherwise, creates a new window.
     *
     * @param name The window name.
     * @hide
     */
    protected fun clearOrCreateWindow(name: String?) {
        window?.clear() ?: run {
            window = windowFactory(name)
        }
    }

    override fun onDeactivateOrClose() {
        super.onDeactivateOrClose()
        window = null
    }
}
