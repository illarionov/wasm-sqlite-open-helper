/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal

/*
 * Original Copyrights:
 * Copyright (C) 2017-2024 requery.io
 * Copyright (C) 2005-2012 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

import android.content.ContentResolver
import android.database.CharArrayBuffer
import android.database.ContentObserver
import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException
import android.database.DataSetObserver
import android.database.StaleDataException
import android.net.Uri
import android.os.Bundle
import ru.pixnews.wasm.sqlite.open.helper.base.ContentObservable
import ru.pixnews.wasm.sqlite.open.helper.base.DataSetObservable
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.CursorWindow
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow
import java.lang.ref.WeakReference
import kotlin.math.max

/**
 * A Cursor implementation that exposes results from a query on a [SQLiteDatabase].
 *
 * SQLiteCursor is not internally synchronized so code using a SQLiteCursor from multiple
 * threads should perform its own synchronization when using the SQLiteCursor.
 *
 * @param query The query object for the cursor
 */
internal class SQLiteCursor(
    private val query: SQLiteQuery,
    rootLogger: Logger,
) : Cursor {
    private val logger = rootLogger.withTag(TAG)
    private var pos: Int = NO_COUNT
    private var closed: Boolean = false

    /** The number of rows in the cursor  */
    private var _count = NO_COUNT

    /** The number of rows that can fit in the cursor window, 0 if unknown  */
    private var cursorWindowCapacity = 0

    /** A mapping of column names to column indices, to speed up lookups  */
    private val columnNameMap: Map<String, Int> by lazy {
        query.columnNames
            .mapIndexed { columnNo, name -> name to columnNo }
            .toMap()
    }

    @Deprecated("deprecated in AOSP but still used for non-deprecated methods")
    private var contentResolver: ContentResolver? = null
    private var notifyUri: Uri? = null
    private val selfObserverLock = Any()
    private var selfObserver: ContentObserver? = null
    private var selfObserverRegistered = false
    private val dataSetObservable = DataSetObservable()
    private val contentObservable = ContentObservable()

    /** Used to find out where a cursor was allocated in case it never got released.  */
    private val closeGuard: CloseGuard = CloseGuard.get()

    /**
     * The cursor window owned by this cursor.
     */
    private var window: CursorWindow? = null
        /**
         * Sets a new cursor window for the cursor to use.
         *
         * The cursor takes ownership of the provided cursor window; the cursor window
         * will be closed when the cursor is closed or when the cursor adopts a new
         * cursor window.
         *
         * If the cursor previously had a cursor window, then it is closed when the
         * new cursor window is assigned.
         *
         * @param newWindow The new cursor window, typically a remote cursor window.
         */
        set(newWindow) {
            if (newWindow !== field) {
                val old = field
                field = newWindow
                old?.close()
            }
            _count = NO_COUNT
        }

    override fun getCount(): Int {
        if (_count == NO_COUNT) {
            fillWindow(0)
        }
        return _count
    }

    override fun getColumnNames(): Array<String> = columnNameMap.keys.toTypedArray<String>()

    override fun getColumnCount(): Int = columnNameMap.size

    override fun getBlob(column: Int): ByteArray {
        checkPosition()
        return requireWindow().getBlob(pos, column) ?: byteArrayOf()
    }

    override fun getString(column: Int): String? {
        checkPosition()
        return requireWindow().getString(pos, column)
    }

    /**
     * Copies the text of the field at the specified row and column index into
     * a [CharArrayBuffer].
     *
     *
     * The buffer is populated as follows:
     *
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
     *  * If the field is of type [Cursor.FIELD_TYPE_BLOB], then [SQLiteException] is thrown.
     *
     */
    override fun copyStringToBuffer(columnIndex: Int, buffer: CharArrayBuffer) {
        val chars = getString(columnIndex)?.toCharArray() ?: charArrayOf()
        buffer.data = chars
        buffer.sizeCopied = chars.size
    }

    override fun getShort(column: Int): Short {
        checkPosition()
        return requireWindow().getShort(pos, column)
    }

    override fun getInt(column: Int): Int {
        checkPosition()
        return requireWindow().getInt(pos, column)
    }

    override fun getLong(column: Int): Long {
        checkPosition()
        return requireWindow().getLong(pos, column)
    }

    override fun getFloat(column: Int): Float {
        checkPosition()
        return requireWindow().getFloat(pos, column)
    }

    override fun getDouble(column: Int): Double {
        checkPosition()
        return requireWindow().getDouble(pos, column)
    }

    override fun isNull(column: Int): Boolean {
        return requireWindow().getType(pos, column) == NativeCursorWindow.CursorFieldType.NULL
    }

    override fun getType(column: Int): Int {
        return requireWindow().getType(pos, column).id
    }

    override fun getPosition(): Int = pos

    /**
     * This function throws CursorIndexOutOfBoundsException if the cursor position is out of bounds.
     * Subclass implementations of the get functions should call this before attempting to
     * retrieve data.
     *
     * @throws CursorIndexOutOfBoundsException
     */
    private fun checkPosition() {
        if (-1 == pos || _count == pos) {
            throw CursorIndexOutOfBoundsException(pos, _count)
        }
        requireWindow()
    }

    private fun requireWindow(): CursorWindow = window ?: throw StaleDataException(
        "Attempting to access a closed CursorWindow." +
                "Most probable cause: cursor is deactivated prior to calling this method.",
    )

    override fun move(offset: Int): Boolean = moveToPosition(pos + offset)

    override fun moveToFirst(): Boolean = moveToPosition(0)

    override fun moveToLast(): Boolean = moveToPosition(count - 1)

    override fun moveToNext(): Boolean = moveToPosition(pos + 1)

    override fun moveToPrevious(): Boolean = moveToPosition(pos - 1)

    override fun isFirst(): Boolean = pos == 0 && count != 0

    override fun isLast(): Boolean = count.let { cnt ->
        pos == (cnt - 1) && cnt != 0
    }

    override fun isBeforeFirst(): Boolean = count == 0 || pos == -1

    override fun isAfterLast(): Boolean = count == 0 || pos == count

    @Suppress("ReturnCount")
    override fun moveToPosition(position: Int): Boolean {
        // Make sure position isn't past the end of the cursor
        val count = count
        if (position >= count) {
            pos = count
            return false
        }

        // Make sure position isn't before the beginning of the cursor
        if (position < 0) {
            pos = -1
            return false
        }

        // Check for no-op moves, and skip the rest of the work for them
        if (position == pos) {
            return true
        }

        window.let {
            if ((it == null) || position !in it.startPosition..<it.startPosition + it.numRows) {
                fillWindow(position)
            }
        }
        pos = position
        return true
    }

    private fun fillWindow(requiredPos: Int) {
        val window = CursorWindow(query.database.path, logger).also {
            this.window = it
        }

        try {
            if (_count == NO_COUNT) {
                val startPos = cursorPickFillWindowStartPosition(requiredPos, 0)
                _count = query.fillWindow(window, startPos, requiredPos, true)
                cursorWindowCapacity = window.numRows

                logger.d { "received count(*) from native_fill_window: $_count" }
            } else {
                val startPos = cursorPickFillWindowStartPosition(requiredPos, cursorWindowCapacity)
                query.fillWindow(window, startPos, requiredPos, false)
            }
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            // Close the cursor window if the query failed and therefore will
            // not produce any results.  This helps to avoid accidentally leaking
            // the cursor window if the client does not correctly handle exceptions
            // and fails to close the cursor.
            this.window = null
            throw ex
        }
    }

    override fun getColumnIndexOrThrow(columnName: String): Int {
        val index = getColumnIndex(columnName)
        require(index >= 0) { "column '$columnName' does not exist" }
        return index
    }

    override fun getColumnIndex(columnName: String): Int {
        // Hack according to some mysterious internal bug903852
        val cleanColumnName = columnName.substringAfterLast(".")
        return columnNameMap.getOrDefault(cleanColumnName, -1)
    }

    override fun getColumnName(columnIndex: Int): String = columnNames[columnIndex]

    override fun registerContentObserver(observer: ContentObserver) {
        contentObservable.registerObserver(observer)
    }

    override fun unregisterContentObserver(observer: ContentObserver) {
        // cursor will unregister all observers when it close
        if (!closed) {
            contentObservable.unregisterObserver(observer)
        }
    }

    override fun registerDataSetObserver(observer: DataSetObserver) {
        dataSetObservable.registerObserver(observer)
    }

    override fun unregisterDataSetObserver(observer: DataSetObserver) {
        dataSetObservable.unregisterObserver(observer)
    }

    /**
     * Specifies a content URI to watch for changes.
     *
     * @param cr The content resolver from the caller's context.
     * @param notifyUri The URI to watch for changes. This can be a
     * specific row URI, or a base URI for a whole class of content.
     */
    override fun setNotificationUri(cr: ContentResolver, notifyUri: Uri) = synchronized(selfObserverLock) {
        this.contentResolver = cr
        this.notifyUri = notifyUri

        selfObserver?.let {
            cr.unregisterContentObserver(it)
        }
        SelfContentObserver(this).let {
            selfObserver = it
            cr.registerContentObserver(notifyUri, true, it)
        }
        selfObserverRegistered = true
    }

    override fun getNotificationUri(): Uri = synchronized(selfObserverLock) { notifyUri!! }

    override fun getWantsAllOnMoveCalls(): Boolean = false

    override fun setExtras(extras: Bundle?) {
        throw UnsupportedOperationException("Extras on cursor not supported")
    }

    override fun getExtras(): Bundle {
        throw UnsupportedOperationException("Extras on cursor not supported")
    }

    override fun respond(extras: Bundle): Bundle {
        throw UnsupportedOperationException("Extras on cursor not supported")
    }

    /**
     * Release the native resources, if they haven't been released yet.
     */
    protected fun finalize() {
        try {
            // if the cursor hasn't been closed yet, close it first
            if (window != null) {
                closeGuard.warnIfOpen()
                close()
            }
        } finally {
            if (selfObserver != null && selfObserverRegistered) {
                contentResolver!!.unregisterContentObserver(selfObserver!!)
            }
            try {
                if (!closed) {
                    close()
                }
            } catch (ignored: Exception) {
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun deactivate() {
        onDeactivateOrClose()
    }

    @Deprecated("Deprecated in Java")
    override fun requery(): Boolean = throw UnsupportedOperationException("Not supported")

    override fun isClosed(): Boolean = closed

    override fun close() {
        closed = true
        contentObservable.unregisterAll()
        onDeactivateOrClose()
        synchronized(this) {
            query.close()
        }
    }

    private fun onDeactivateOrClose() {
        if (selfObserver != null) {
            contentResolver!!.unregisterContentObserver(selfObserver!!)
            selfObserverRegistered = false
        }
        dataSetObservable.notifyInvalidated()
        window = null
    }

    /**
     * Subclasses must call this method when they finish committing updates to notify all
     * observers.
     */
    private fun onChange() {
        synchronized(selfObserverLock) {
            contentObservable.dispatchChange(false, null)
        }
    }

    /**
     * Cursors use this class to track changes others make to their URI.
     */
    private class SelfContentObserver(cursor: SQLiteCursor) : ContentObserver(null) {
        var cursor: WeakReference<SQLiteCursor> = WeakReference(cursor)

        override fun deliverSelfNotifications(): Boolean = false

        override fun onChange(selfChange: Boolean) {
            val cursor = cursor.get()
            cursor?.onChange()
        }
    }

    private companion object {
        const val TAG: String = "SQLiteCursor"
        const val NO_COUNT: Int = -1
        fun cursorPickFillWindowStartPosition(
            cursorPosition: Int,
            cursorWindowCapacity: Int,
        ): Int {
            @Suppress("MagicNumber")
            return max((cursorPosition - cursorWindowCapacity / 3).toDouble(), 0.0).toInt()
        }
    }
}
