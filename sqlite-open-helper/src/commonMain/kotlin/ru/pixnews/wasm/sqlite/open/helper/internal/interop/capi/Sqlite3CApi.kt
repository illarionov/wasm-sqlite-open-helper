/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.interop.capi

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.EmbedderMemory
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.SqliteDatabaseResourcesRegistry

internal class Sqlite3CApi(
    sqliteBindings: SqliteBindings,
    memory: EmbedderMemory,
    callbackStore: SqliteCallbackStore,
    callbackFunctionIndexes: Sqlite3CallbackFunctionIndexes,
    databaseResourcesRegistry: SqliteDatabaseResourcesRegistry,
    rootLogger: Logger,
) {
    val config = Sqlite3ConfigFunctions(sqliteBindings, callbackStore, callbackFunctionIndexes)
    val errors = Sqlite3ErrorFunctions(sqliteBindings, memory)
    val db = Sqlite3DbFunctions(
        sqliteBindings = sqliteBindings,
        memory = memory,
        callbackStore = callbackStore,
        callbackFunctionIndexes = callbackFunctionIndexes,
        databaseResources = databaseResourcesRegistry,
        sqliteErrorApi = errors,
        rootLogger = rootLogger,
    )
    val statement = Sqlite3StatementFunctions(
        sqliteBindings = sqliteBindings,
        memory = memory,
        databaseResourcesRegistry = databaseResourcesRegistry,
        sqliteErrorApi = errors,
    )
}
