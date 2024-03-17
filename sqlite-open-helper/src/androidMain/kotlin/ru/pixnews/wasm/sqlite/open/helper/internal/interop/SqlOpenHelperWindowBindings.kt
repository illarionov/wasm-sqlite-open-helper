/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.interop

internal interface SqlOpenHelperWindowBindings<WP : Sqlite3WindowPtr> {
    fun nativeCreate(name: String, cursorWindowSize: Int): WP?
    fun nativeDispose(windowPtr: WP)

    fun nativeClear(windowPtr: WP)

    fun nativeGetNumRows(windowPtr: WP): Int
    fun nativeSetNumColumns(windowPtr: WP, columnNum: Int): Boolean
    fun nativeAllocRow(windowPtr: WP): Boolean
    fun nativeFreeLastRow(windowPtr: WP)

    fun nativeGetType(windowPtr: WP, row: Int, column: Int): NativeCursorWindow.CursorFieldType
    fun nativeGetBlob(windowPtr: WP, row: Int, column: Int): ByteArray?
    fun nativeGetString(windowPtr: WP, row: Int, column: Int): String?
    fun nativeGetLong(windowPtr: WP, row: Int, column: Int): Long
    fun nativeGetDouble(windowPtr: WP, row: Int, column: Int): Double

    fun nativePutBlob(windowPtr: WP, value: ByteArray, row: Int, column: Int): Boolean
    fun nativePutString(windowPtr: WP, value: String, row: Int, column: Int): Boolean
    fun nativePutLong(windowPtr: WP, value: Long, row: Int, column: Int): Boolean
    fun nativePutDouble(windowPtr: WP, value: Double, row: Int, column: Int): Boolean
    fun nativePutNull(windowPtr: WP, row: Int, column: Int): Boolean

    fun nativeGetName(windowPtr: WP): String
}
