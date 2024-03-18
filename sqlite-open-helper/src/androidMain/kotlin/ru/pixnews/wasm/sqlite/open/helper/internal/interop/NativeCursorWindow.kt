/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.interop

/*
 * Original Copyrights:
 * Copyright (C) 2017-2024 requery.io
 * Copyright (C) 2005-2012 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.NativeCursorWindow.CursorFieldType.BLOB
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.NativeCursorWindow.CursorFieldType.FLOAT
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.NativeCursorWindow.CursorFieldType.INTEGER
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.NativeCursorWindow.CursorFieldType.NULL
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.NativeCursorWindow.CursorFieldType.STRING
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.NativeCursorWindow.Field.BlobField
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.NativeCursorWindow.Field.FloatField
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.NativeCursorWindow.Field.IntegerField
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.NativeCursorWindow.Field.Null
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.NativeCursorWindow.Field.StringField

internal class NativeCursorWindow(
    val name: String,
    val size: Int,
    val isReadOnly: Boolean = false,
    rootLogger: Logger,
) {
    private val logger = rootLogger.withTag("NativeCursorWindow")
    private val data: Header = Header(0, RowSlotChunk(), 0, 0)
    private var _freeSpace: Int = size

    val freeSpace: Int
        get() = _freeSpace

    val numRows: Int
        get() = data.numRows

    fun clear(): Int {
        if (isReadOnly) {
            return -1
        }
        data.freeOffset = 0
        data.firstChunk = RowSlotChunk()
        data.numColumns = 0
        data.numRows = 0
        return 0
    }

    fun setNumColumns(numColumns: Int): Int {
        if (isReadOnly) {
            return -1
        }

        data.numColumns.let { columnNo ->
            if ((columnNo > 0 || data.numRows > 0) && numColumns != columnNo) {
                logger.i { "Trying to go from $columnNo columns to $numColumns" }
                return -1
            }
        }

        data.numColumns = numColumns
        return 0
    }

    fun allocRow(): Int {
        check(!isReadOnly)
        allocRowSlot().apply {
            fields = Array(data.numColumns) { FieldSlot() }
        }
        // TODO fail on full window
        return 0
    }

    fun freeLastRow() {
        check(!isReadOnly)
        if (data.numRows > 0) {
            data.numRows -= 1
        }
    }

    private fun allocRowSlot(): RowSlot {
        check(!isReadOnly)

        var chunkPos = data.numRows
        var rowSlotChunk: RowSlotChunk = data.firstChunk
        while (chunkPos > ROW_SLOT_CHUNK_NUM_ROWS) {
            rowSlotChunk = rowSlotChunk.nextChunk!!
            chunkPos -= ROW_SLOT_CHUNK_NUM_ROWS
        }
        if (chunkPos == ROW_SLOT_CHUNK_NUM_ROWS) {
            RowSlotChunk().let {
                rowSlotChunk.nextChunk = it
                rowSlotChunk = it
            }
            chunkPos = 0
        }
        data.numRows += 1
        return rowSlotChunk.slots[chunkPos]
    }

    private fun getRowSlot(row: Int): RowSlot? {
        var chunkPos = row
        var rowSlotChunk: RowSlotChunk? = data.firstChunk
        while (chunkPos >= ROW_SLOT_CHUNK_NUM_ROWS) {
            rowSlotChunk = rowSlotChunk?.nextChunk
            chunkPos -= ROW_SLOT_CHUNK_NUM_ROWS
        }
        return rowSlotChunk?.let { it.slots[chunkPos] }
    }

    fun getFieldSlot(row: Int, column: Int): FieldSlot? {
        if (row >= data.numRows || column >= data.numColumns) {
            logger.e {
                "Failed to read row $row, column $column from a CursorWindow which " +
                        "has ${data.numRows} rows, ${data.numColumns} columns"
            }
            return null
        }
        val slot = getRowSlot(row) ?: run {
            logger.e { "Failed to find rowSlot for row $row" }
            return null
        }
        return slot.fields[column]
    }

    fun putBlob(row: Int, column: Int, value: ByteArray): Int {
        check(!isReadOnly)
        return putField(row, column, BlobField(value))
    }

    fun putString(row: Int, column: Int, value: String): Int = putField(row, column, StringField(value))

    fun putLong(row: Int, column: Int, value: Long): Int = putField(row, column, IntegerField(value))

    fun putDouble(row: Int, column: Int, value: Double): Int = putField(row, column, FloatField(value))
    fun putNull(row: Int, column: Int): Int = putField(row, column, Null)

    @Suppress("COMPACT_OBJECT_INITIALIZATION")
    private fun putField(row: Int, column: Int, value: Field): Int {
        check(!isReadOnly)
        val slot: FieldSlot = getFieldSlot(row, column) ?: return -1 // BAD_VALUE
        slot.field = value
        return 0
    }

    /**
     * @property freeOffset
     *   Offset of the lowest unused byte in the window
     * @property firstChunk firstChunkOffset
     */
    private class Header(
        var freeOffset: Long,
        var firstChunk: RowSlotChunk,

        var numRows: Int,
        var numColumns: Int,
    )

    sealed class Field(
        val type: CursorFieldType,
    ) {
        data object Null : Field(NULL)
        class IntegerField(val value: Long) : Field(INTEGER)
        class FloatField(val value: Double) : Field(FLOAT)
        class StringField(val value: String) : Field(STRING)
        class BlobField(val value: ByteArray) : Field(BLOB)
    }

    class FieldSlot(
        var field: Field = Null,
    ) {
        val type: CursorFieldType
            get() = this.field.type
    }

    private class RowSlot(numColumns: Int) {
        var fields: Array<FieldSlot> = Array(numColumns) { FieldSlot() }
    }

    private class RowSlotChunk {
        val slots: MutableList<RowSlot> = MutableList(ROW_SLOT_CHUNK_NUM_ROWS) { RowSlot(0) }
        var nextChunk: RowSlotChunk? = null
    }

    @Suppress("MagicNumber")
    enum class CursorFieldType(val id: Int) {
        NULL(0),
        INTEGER(1),
        FLOAT(2),
        STRING(3),
        BLOB(4),
    }

    companion object {
        private const val ROW_SLOT_CHUNK_NUM_ROWS = 100
    }
}
