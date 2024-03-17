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

import android.content.ContentResolver
import android.database.CharArrayBuffer
import android.database.ContentObserver
import android.database.Cursor
import android.database.CursorIndexOutOfBoundsException
import android.database.DataSetObserver
import android.net.Uri
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * This is an abstract cursor class that handles a lot of the common code
 * that all cursors need to deal with and is provided for convenience reasons.
 */
internal abstract class AbstractCursor : Cursor {
    protected var pos: Int = -1
    protected var closed: Boolean = false

    @Deprecated("deprecated in AOSP but still used for non-deprecated methods")
    private var contentResolver: ContentResolver? = null
    private var notifyUri: Uri? = null
    private val selfObserverLock = Any()
    private var selfObserver: ContentObserver? = null
    private var selfObserverRegistered = false
    private val dataSetObservable = DataSetObservable()
    private val contentObservable = ContentObservable()

    abstract override fun getCount(): Int

    abstract override fun getColumnNames(): Array<String>

    abstract override fun getString(column: Int): String?
    abstract override fun getShort(column: Int): Short
    abstract override fun getInt(column: Int): Int
    abstract override fun getLong(column: Int): Long
    abstract override fun getFloat(column: Int): Float
    abstract override fun getDouble(column: Int): Double
    abstract override fun isNull(column: Int): Boolean

    abstract override fun getType(column: Int): Int

    override fun getBlob(column: Int): ByteArray {
        throw UnsupportedOperationException("getBlob is not supported")
    }

    override fun getColumnCount(): Int = columnNames.size

    @Deprecated("Deprecated in Java")
    override fun deactivate() {
        onDeactivateOrClose()
    }

    /** @hide
     */
    protected open fun onDeactivateOrClose() {
        if (selfObserver != null) {
            contentResolver!!.unregisterContentObserver(selfObserver!!)
            selfObserverRegistered = false
        }
        dataSetObservable.notifyInvalidated()
    }

    @Deprecated("Deprecated in Java")
    override fun requery(): Boolean = throw UnsupportedOperationException("Not supported")

    override fun isClosed(): Boolean = closed

    override fun close() {
        closed = true
        contentObservable.unregisterAll()
        onDeactivateOrClose()
    }

    override fun copyStringToBuffer(columnIndex: Int, buffer: CharArrayBuffer) {
        // Default implementation, uses getString
        val result = getString(columnIndex)
        if (result != null) {
            val data = buffer.data
            if (data == null || data.size < result.length) {
                buffer.data = result.toCharArray()
            } else {
                result.toCharArray(data, 0, 0, result.length)
            }
            buffer.sizeCopied = result.length
        } else {
            buffer.sizeCopied = 0
        }
    }

    override fun getPosition(): Int {
        return pos
    }

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

        val result = onMove(pos, position)
        pos = if (!result) {
            -1
        } else {
            position
        }

        return result
    }

    /**
     * This function is called every time the cursor is successfully scrolled
     * to a new position, giving the subclass a chance to update any state it
     * may have.  If it returns false the move function will also do so and the
     * cursor will scroll to the beforeFirst position.
     *
     *
     * This function should be called by methods such as [.moveToPosition],
     * so it will typically not be called from outside of the cursor class itself.
     *
     *
     * @param oldPosition The position that we're moving from.
     * @param newPosition The position that we're moving to.
     * @return True if the move is successful, false otherwise.
     */
    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    abstract fun onMove(oldPosition: Int, newPosition: Int): Boolean

    override fun move(offset: Int): Boolean {
        return moveToPosition(pos + offset)
    }

    override fun moveToFirst(): Boolean {
        return moveToPosition(0)
    }

    override fun moveToLast(): Boolean {
        return moveToPosition(count - 1)
    }

    override fun moveToNext(): Boolean {
        return moveToPosition(pos + 1)
    }

    override fun moveToPrevious(): Boolean {
        return moveToPosition(pos - 1)
    }

    override fun isFirst(): Boolean {
        return pos == 0 && count != 0
    }

    override fun isLast(): Boolean {
        val cnt = count
        return pos == (cnt - 1) && cnt != 0
    }

    override fun isBeforeFirst(): Boolean {
        return count == 0 || pos == -1
    }

    override fun isAfterLast(): Boolean {
        return count == 0 || pos == count
    }

    override fun getColumnIndex(columnName: String): Int {
        // Hack according to some mysterious internal bug903852
        val tableColumnName = columnName.substringAfterLast(".")
        return columnNames.indexOfFirst { it.equals(tableColumnName, ignoreCase = true) }
    }

    override fun getColumnIndexOrThrow(columnName: String): Int {
        val index = getColumnIndex(columnName)
        require(index >= 0) { "column '$columnName' does not exist" }
        return index
    }

    override fun getColumnName(columnIndex: Int): String {
        return columnNames[columnIndex]
    }

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
     * Subclasses must call this method when they finish committing updates to notify all
     * observers.
     *
     * @param selfChange value
     */
    protected fun onChange(selfChange: Boolean) {
        synchronized(selfObserverLock) {
            contentObservable.dispatchChange(selfChange, null)
            if (notifyUri != null && selfChange) {
                contentResolver!!.notifyChange(notifyUri!!, selfObserver)
            }
        }
    }

    /**
     * Specifies a content URI to watch for changes.
     *
     * @param cr The content resolver from the caller's context.
     * @param notifyUri The URI to watch for changes. This can be a
     * specific row URI, or a base URI for a whole class of content.
     */
    override fun setNotificationUri(cr: ContentResolver, notifyUri: Uri) = synchronized(selfObserverLock) {
        this.notifyUri = notifyUri
        contentResolver = cr
        if (selfObserver != null) {
            contentResolver!!.unregisterContentObserver(selfObserver!!)
        }
        SelfContentObserver(this).let {
            selfObserver = it
            contentResolver!!.registerContentObserver(this.notifyUri!!, true, it)
        }
        selfObserverRegistered = true
    }

    override fun getNotificationUri(): Uri = synchronized(selfObserverLock) {
        return notifyUri!!
    }

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
     * This function throws CursorIndexOutOfBoundsException if the cursor position is out of bounds.
     * Subclass implementations of the get functions should call this before attempting to
     * retrieve data.
     *
     * @throws CursorIndexOutOfBoundsException
     */
    protected open fun checkPosition() {
        if (-1 == pos || count == pos) {
            throw CursorIndexOutOfBoundsException(pos, count)
        }
    }

    protected open fun finalize() {
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

    /**
     * Cursors use this class to track changes others make to their URI.
     */
    protected class SelfContentObserver(cursor: AbstractCursor) : ContentObserver(null) {
        var cursor: WeakReference<AbstractCursor> = WeakReference(cursor)

        override fun deliverSelfNotifications(): Boolean {
            return false
        }

        override fun onChange(selfChange: Boolean) {
            val cursor = cursor.get()
            cursor?.onChange(false)
        }
    }
}
