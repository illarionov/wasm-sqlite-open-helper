/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi.databaseresources

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteStatement

internal class SqliteDatabaseResourcesRegistry(
    private val callbackStore: SqliteCallbackStore,
    rootLogger: Logger,
) {
    private val logger: Logger = rootLogger.withTag("SqliteOpenDatabaseResources")
    private val lock = Any()
    private val openedDatabases: MutableMap<WasmPtr<SqliteDb>, DbResources> = mutableMapOf()

    fun onDbOpened(
        db: WasmPtr<SqliteDb>,
    ): Unit = synchronized(lock) {
        val old = openedDatabases.putIfAbsent(db, DbResources())
        if (old != null) {
            error("Database $db already registered")
        }
    }

    fun registerStatement(
        db: WasmPtr<SqliteDb>,
        statement: WasmPtr<SqliteStatement>,
    ): Unit = synchronized(lock) {
        val dbResources = openedDatabases[db] ?: run {
            logger.e { "Database $db not registered" }
            return
        }
        if (!dbResources.statements.add(statement)) {
            logger.e { "Statement $statement already registered on database $db" }
        }
    }

    fun unregisterStatement(
        db: WasmPtr<SqliteDb>,
        statement: WasmPtr<SqliteStatement>,
    ): Unit = synchronized(lock) {
        val dbResources = openedDatabases[db] ?: run {
            logger.e { "unregisterStatement(): Database $db not registered" }
            return
        }
        if (!dbResources.statements.remove(statement)) {
            logger.e { "unregisterStatement(): Statement $statement not registered" }
        }
    }

    fun afterDbClosed(closedDb: WasmPtr<SqliteDb>): Unit = synchronized(lock) {
        val db = openedDatabases.remove(closedDb)
        if (db == null) {
            logger.e { "afterDbClosed(): database $closedDb not registered" }
        } else {
            if (db.statements.isNotEmpty()) {
                logger.w {
                    "afterDbClosed(): prepared statements are not closed: " +
                            "${db.statements.joinToString(", ", limit = 5)} "
                }
            }
        }

        callbackStore.sqlite3TraceCallbacks.remove(closedDb)
        callbackStore.sqlite3ProgressCallbacks.remove(closedDb)
    }

    private class DbResources {
        val statements: MutableSet<WasmPtr<SqliteStatement>> = mutableSetOf()
    }
}
