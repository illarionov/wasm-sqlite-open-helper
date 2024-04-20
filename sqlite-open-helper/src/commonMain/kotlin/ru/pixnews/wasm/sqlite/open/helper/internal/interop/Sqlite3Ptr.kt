/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.interop

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.isSqlite3Null
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteStatement

internal interface Sqlite3Ptr {
    fun isNull(): Boolean
}

internal interface Sqlite3ConnectionPtr : Sqlite3Ptr
internal interface Sqlite3StatementPtr : Sqlite3Ptr

@JvmInline
internal value class WasmSqlite3StatementPtr(
    val ptr: WasmPtr<SqliteStatement>,
) : Sqlite3StatementPtr {
    override fun isNull(): Boolean = ptr.isSqlite3Null()
}

@JvmInline
internal value class WasmSqlite3ConnectionPtr(
    val ptr: WasmPtr<SqliteDb>,
) : Sqlite3ConnectionPtr {
    override fun isNull(): Boolean = ptr.isSqlite3Null()
}
