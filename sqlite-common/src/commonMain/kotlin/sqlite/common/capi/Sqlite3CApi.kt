/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.sqlite.common.capi

import at.released.wasm.sqlite.open.helper.embedder.SqliteEmbedderRuntimeInfo
import at.released.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import at.released.wasm.sqlite.open.helper.embedder.exports.SqliteExports
import at.released.wasm.sqlite.open.helper.embedder.functiontable.SqliteCallbackFunctionIndexes
import at.released.wasm.sqlite.open.helper.sqlite.common.capi.databaseresources.SqliteDatabaseResourcesRegistry
import at.released.weh.common.api.Logger
import at.released.weh.wasm.core.memory.Memory

public class Sqlite3CApi(
    sqliteExports: SqliteExports,
    public val embedderInfo: SqliteEmbedderRuntimeInfo,
    memory: Memory,
    callbackStore: SqliteCallbackStore,
    callbackFunctionIndexes: SqliteCallbackFunctionIndexes,
    rootLogger: Logger,
) {
    private val databaseResourcesRegistry = SqliteDatabaseResourcesRegistry(callbackStore, rootLogger)
    public val config: Sqlite3ConfigFunctions = Sqlite3ConfigFunctions(
        sqliteExports,
        callbackStore,
        callbackFunctionIndexes,
    )
    public val errors: Sqlite3ErrorFunctions = Sqlite3ErrorFunctions(sqliteExports, memory)
    public val db: Sqlite3DbFunctions = Sqlite3DbFunctions(
        sqliteExports = sqliteExports,
        memory = memory,
        callbackStore = callbackStore,
        callbackFunctionIndexes = callbackFunctionIndexes,
        databaseResources = databaseResourcesRegistry,
        sqliteErrorApi = errors,
        rootLogger = rootLogger,
    )
    public val statement: Sqlite3StatementFunctions = Sqlite3StatementFunctions(
        sqliteExports = sqliteExports,
        memory = memory,
        databaseResourcesRegistry = databaseResourcesRegistry,
        sqliteErrorApi = errors,
    )
}
