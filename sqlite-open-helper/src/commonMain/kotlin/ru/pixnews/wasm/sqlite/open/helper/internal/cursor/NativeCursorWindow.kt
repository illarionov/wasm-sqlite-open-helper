/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.cursor

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.CursorFieldType.BLOB
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.CursorFieldType.FLOAT
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.CursorFieldType.INTEGER
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.CursorFieldType.NULL
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.CursorFieldType.STRING
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.Field.Null

internal class NativeCursorWindow(
    val name: String,
    val size: Int,
    rootLogger: Logger,
) {
    private val logger = rootLogger.withTag("NativeCursorWindow")
    private val data: Header = Header(0, ArrayDeque(), 0, 0)
    private var _freeSpace: Int = size

    val freeSpace: Int
        get() = _freeSpace

    val numRows: Int
        get() = data.numRows

    fun clear(): Int {
        data.freeOffset = 0
        data.chunks = ArrayDeque()
        data.numColumns = 0
        data.numRows = 0
        return 0
    }

    fun setNumColumns(numColumns: Int): Int {
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
        allocRowSlot().apply {
            fields = Array(data.numColumns) { Null }
        }
        // TODO fail on full window
        return 0
    }

    fun freeLastRow() {
        if (data.numRows > 0) {
            data.numRows -= 1
        }
    }

    fun getField(row: Int, column: Int): Field? {
        if (!isValidRowColumn(row, column)) {
            return null
        }
        val slot = getRowSlot(row) ?: run {
            logger.e { "Failed to find rowSlot for row $row" }
            return null
        }
        return slot.fields[column]
    }

    fun putField(row: Int, column: Int, value: Field): Int {
        if (!isValidRowColumn(row, column)) {
            return -1
        } // BAD_VALUE
        val rowSlot = getRowSlot(row) ?: run {
            logger.e { "Failed to find rowSlot for row $row" }
            return -1
        }
        rowSlot.fields[column] = value
        return 0
    }

    private fun allocRowSlot(): RowSlot {
        val chunkPos = data.numRows
        val slotPos = chunkPos / ROW_SLOT_CHUNK_NUM_ROWS
        if (slotPos > data.chunks.lastIndex) {
            data.chunks.addLast(RowSlotChunk())
        }
        data.numRows += 1
        return data.chunks[slotPos].slots[chunkPos % ROW_SLOT_CHUNK_NUM_ROWS]
    }

    private fun getRowSlot(row: Int): RowSlot? {
        val pos = row / ROW_SLOT_CHUNK_NUM_ROWS
        return data.chunks.getOrNull(pos)?.slots?.get(row % ROW_SLOT_CHUNK_NUM_ROWS)
    }

    private fun isValidRowColumn(row: Int, column: Int): Boolean {
        return if (row >= data.numRows || column >= data.numColumns) {
            logger.e {
                "Failed to read row $row, column $column from a CursorWindow which " +
                        "has ${data.numRows} rows, ${data.numColumns} columns"
            }
            false
        } else {
            true
        }
    }

    private class Header(
        var freeOffset: Long,
        var chunks: ArrayDeque<RowSlotChunk>,

        var numRows: Int,
        var numColumns: Int,
    )

    private class RowSlotChunk {
        val slots: MutableList<RowSlot> = MutableList(ROW_SLOT_CHUNK_NUM_ROWS) { RowSlot(0) }
    }

    private class RowSlot(numColumns: Int) {
        var fields: Array<Field> = Array(numColumns) { Null }
    }

    sealed class Field(
        val type: CursorFieldType,
    ) {
        data object Null : Field(NULL)
        class IntegerField(val value: Long) : Field(INTEGER)
        class FloatField(val value: Double) : Field(FLOAT)
        class StringField(val value: String) : Field(STRING)
        class BlobField(val value: ByteArray) : Field(BLOB)
    }

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
