/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.interop

import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteOpenFlags

internal interface SqlOpenHelperNativeBindings<
        CP : Sqlite3ConnectionPtr,
        SP : Sqlite3StatementPtr,
        > {
    fun connectionNullPtr(): CP
    fun connectionStatementPtr(): SP

    fun nativeOpen(
        path: String,
        openFlags: SqliteOpenFlags,
        label: String,
        enableTrace: Boolean,
        enableProfile: Boolean,
    ): CP

    fun nativeClose(
        connectionPtr: CP,
    )

    fun nativePrepareStatement(
        connectionPtr: CP,
        sql: String,
    ): SP

    fun nativeFinalizeStatement(
        connectionPtr: CP,
        statementPtr: SP,
    )

    fun nativeGetParameterCount(
        connectionPtr: CP,
        statementPtr: SP,
    ): Int

    fun nativeIsReadOnly(
        connectionPtr: CP,
        statementPtr: SP,
    ): Boolean

    fun nativeGetColumnCount(
        connectionPtr: CP,
        statementPtr: SP,
    ): Int

    fun nativeGetColumnName(
        connectionPtr: CP,
        statementPtr: SP,
        index: Int,
    ): String?

    fun nativeBindNull(
        connectionPtr: CP,
        statementPtr: SP,
        index: Int,
    )

    fun nativeBindLong(
        connectionPtr: CP,
        statementPtr: SP,
        index: Int,
        value: Long,
    )

    fun nativeBindDouble(
        connectionPtr: CP,
        statementPtr: SP,
        index: Int,
        value: Double,
    )

    fun nativeBindString(
        connectionPtr: CP,
        statementPtr: SP,
        index: Int,
        value: String,
    )

    fun nativeBindBlob(
        connectionPtr: CP,
        statementPtr: SP,
        index: Int,
        value: ByteArray,
    )

    fun nativeResetStatementAndClearBindings(
        connectionPtr: CP,
        statementPtr: SP,
    )

    fun nativeExecute(
        connectionPtr: CP,
        statementPtr: SP,
    )

    fun nativeExecuteForLong(
        connectionPtr: CP,
        statementPtr: SP,
    ): Long

    fun nativeExecuteForString(
        connectionPtr: CP,
        statementPtr: SP,
    ): String?

    fun nativeExecuteForChangedRowCount(
        connectionPtr: CP,
        statementPtr: SP,
    ): Int

    fun nativeExecuteForLastInsertedRowId(
        connectionPtr: CP,
        statementPtr: SP,
    ): Long

    fun nativeExecuteForCursorWindow(
        connectionPtr: CP,
        statementPtr: SP,
        window: NativeCursorWindow,
        startPos: Int,
        requiredPos: Int,
        countAllRows: Boolean,
    ): Long

    fun nativeGetDbLookaside(
        connectionPtr: CP,
    ): Int

    fun nativeCancel(
        connectionPtr: CP,
    )

    fun nativeResetCancel(
        connectionPtr: CP,
        cancelable: Boolean,
    )
}
