/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.util

import android.database.Cursor

fun Cursor.readValues(): List<Map<String, String?>> = this.use { cursor ->
    val rows: MutableList<Map<String, String?>> = mutableListOf()
    while (cursor.moveToNext()) {
        val row = (0 until cursor.columnCount).associateBy(cursor::getColumnName) { columnIndex ->
            if (!cursor.isNull(columnIndex)) {
                cursor.getString(columnIndex)
            } else {
                null
            }
        }
        rows.add(row)
    }
    return rows
}
