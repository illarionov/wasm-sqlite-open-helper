/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb

internal class Sqlite3ConnectionRegistry {
    private val map: MutableMap<WasmPtr<SqliteDb>, SqliteConnection> = mutableMapOf()

    fun add(
        dbPtr: WasmPtr<SqliteDb>,
        path: String,
    ): SqliteConnection {
        val connection = SqliteConnection(dbPtr, path, false)
        val old = map.put(dbPtr, connection)
        check(old == null) { "Connection $dbPtr already registered" }
        return connection
    }

    fun get(ptr: WasmPtr<SqliteDb>): SqliteConnection? = map[ptr]

    fun remove(ptr: WasmPtr<SqliteDb>): SqliteConnection? = map.remove(ptr)

    internal class SqliteConnection(
        val dbPtr: WasmPtr<SqliteDb>,
        val path: String,
        var isCancelled: Boolean = false,
    )
}
