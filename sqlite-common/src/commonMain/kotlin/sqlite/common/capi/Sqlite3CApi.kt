/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.EmbedderMemory
import ru.pixnews.wasm.sqlite.open.helper.embedder.SQLiteEmbedderRuntimeInfo
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi.databaseresources.SqliteDatabaseResourcesRegistry

public class Sqlite3CApi(
    sqliteBindings: SqliteBindings,
    public val embedderInfo: SQLiteEmbedderRuntimeInfo,
    memory: EmbedderMemory,
    callbackStore: SqliteCallbackStore,
    callbackFunctionIndexes: Sqlite3CallbackFunctionIndexes,
    rootLogger: Logger,
) {
    private val databaseResourcesRegistry = SqliteDatabaseResourcesRegistry(callbackStore, rootLogger)
    public val config: Sqlite3ConfigFunctions = Sqlite3ConfigFunctions(
        sqliteBindings,
        callbackStore,
        callbackFunctionIndexes,
    )
    public val errors: Sqlite3ErrorFunctions = Sqlite3ErrorFunctions(sqliteBindings, memory)
    public val db: Sqlite3DbFunctions = Sqlite3DbFunctions(
        sqliteBindings = sqliteBindings,
        memory = memory,
        callbackStore = callbackStore,
        callbackFunctionIndexes = callbackFunctionIndexes,
        databaseResources = databaseResourcesRegistry,
        sqliteErrorApi = errors,
        rootLogger = rootLogger,
    )
    public val statement: Sqlite3StatementFunctions = Sqlite3StatementFunctions(
        sqliteBindings = sqliteBindings,
        memory = memory,
        databaseResourcesRegistry = databaseResourcesRegistry,
        sqliteErrorApi = errors,
    )
}
