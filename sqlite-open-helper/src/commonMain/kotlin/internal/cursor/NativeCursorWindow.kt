/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.cursor

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.encodedNullTerminatedStringLength
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.CursorFieldType.BLOB
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.CursorFieldType.FLOAT
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.CursorFieldType.INTEGER
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.CursorFieldType.NULL
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.CursorFieldType.STRING
import ru.pixnews.wasm.sqlite.open.helper.internal.cursor.NativeCursorWindow.Field.Null
import kotlin.LazyThreadSafetyMode.NONE

internal class NativeCursorWindow(
    val name: String,
    val size: Int,
    rootLogger: Logger,
) {
    private val logger = rootLogger.withTag("NativeCursorWindow")
    private val rows: ArrayDeque<RowSlot> = ArrayDeque()
    private var numColumns: Int = 0
    var freeSpace: Int = size
        private set

    val numRows: Int
        get() = rows.size

    fun setNumColumns(numColumns: Int): Int {
        if ((this.numColumns > 0 || this.numRows > 0) && numColumns != this.numColumns) {
            logger.i { "Trying to go from ${this.numRows} columns to $numColumns" }
            return -1
        }
        this.numColumns = numColumns
        return 0
    }

    fun allocRow(): Int {
        val rowSlotSize = numColumns * SLOT_SIZE_BYTES
        if (freeSpace - rowSlotSize < 0) {
            return -1 // NO_MEMORY
        }
        freeSpace -= rowSlotSize
        RowSlot(numColumns).also {
            rows.addLast(it)
        }
        return 0
    }

    fun freeLastRow() {
        if (rows.isNotEmpty()) {
            val lastSlot = rows.removeLast()
            freeSpace += lastSlot.fields.size * SLOT_SIZE_BYTES + lastSlot.payloadSize
        }
    }

    fun getField(row: Int, column: Int): Field? {
        if (!isValidRowColumn(row, column)) {
            return null
        }
        val slot = rows.getOrNull(row) ?: run {
            logger.e { "Failed to find rowSlot for row $row" }
            return null
        }
        return slot.fields[column]
    }

    @Suppress("ReturnCount")
    fun putField(row: Int, column: Int, value: Field): Int {
        if (!isValidRowColumn(row, column)) {
            return -1
        } // BAD_VALUE

        val rowSlot = rows.getOrNull(row) ?: run {
            logger.e { "Failed to find rowSlot for row $row" }
            return -1
        }

        val additionalSpace = value.payloadSize
        if (freeSpace < additionalSpace) {
            return -1 // NO_MEMORY
        }

        rowSlot.fields[column] = value
        freeSpace -= additionalSpace

        return 0
    }

    fun clear(): Int {
        rows.clear()
        freeSpace = size
        return 0
    }

    private fun isValidRowColumn(row: Int, column: Int): Boolean {
        return if (row >= this.numRows || column >= this.numColumns) {
            logger.e {
                "Failed to read row $row, column $column from a CursorWindow which " +
                        "has ${this.numRows} rows, ${this.numColumns} columns"
            }
            false
        } else {
            true
        }
    }

    private class RowSlot(numColumns: Int) {
        val fields: Array<Field> = Array(numColumns) { Null }

        @Suppress("WRONG_NAME_OF_VARIABLE_INSIDE_ACCESSOR")
        val payloadSize: Int
            get() = fields.sumOf(Field::payloadSize)
    }

    sealed class Field(
        val type: CursorFieldType,
    ) {
        open val payloadSize: Int = 0

        data object Null : Field(NULL)

        class IntegerField(public val value: Long) : Field(INTEGER)

        class FloatField(public val value: Double) : Field(FLOAT)

        class StringField(public val value: String) : Field(STRING) {
            override val payloadSize: Int by lazy(NONE) {
                value.encodedNullTerminatedStringLength()
            }
        }
        class BlobField(public val value: ByteArray) : Field(BLOB) {
            override val payloadSize: Int = value.size
        }
    }

    enum class CursorFieldType(public val id: Int) {
        NULL(0),
        INTEGER(1),
        FLOAT(2),
        STRING(3),
        BLOB(4),
    }

    private companion object {
        private const val SLOT_SIZE_BYTES = 16
    }
}
