/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver

import androidx.sqlite.SQLiteDriver
import ru.pixnews.wasm.sqlite.driver.dsl.DebugConfigBlock
import ru.pixnews.wasm.sqlite.driver.dsl.OpenParamsBlock
import ru.pixnews.wasm.sqlite.driver.dsl.WasmSqliteDriverConfigBlock
import ru.pixnews.wasm.sqlite.driver.internal.WasmSqliteDriver
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteWasmEnvironment
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3CApi

/**
* Creates a [SQLiteDriver] with the specified [block] configuration.
*
* @param embedder WebAssembly Runtime (embedder) for running SQLite compiled to Wasm.
* For example, GraalvmSqliteEmbedder
*/
@Suppress("FunctionName")
public fun <E : SqliteEmbedderConfig> WasmSQLiteDriver(
    embedder: SqliteEmbedder<E>,
    block: WasmSqliteDriverConfigBlock<E>.() -> Unit,
): SQLiteDriver {
    val config = WasmSqliteDriverConfigBlock<E>().apply(block)
    val commonConfig = object : WasmSqliteCommonConfig {
        override val logger: Logger = config.logger
    }

    val callbackStore = SqliteCallbackStore()
    val embedderEnv: SqliteWasmEnvironment = embedder.createSqliteWasmEnvironment(
        commonConfig = commonConfig,
        callbackStore = callbackStore,
        embedderConfigBuilder = config.embedderConfig,
    )
    val cApi = Sqlite3CApi(
        sqliteBindings = embedderEnv.sqliteBindings,
        embedderInfo = embedderEnv.embedderInfo,
        memory = embedderEnv.memory,
        callbackStore = callbackStore,
        callbackFunctionIndexes = embedderEnv.callbackFunctionIndexes,
        rootLogger = config.logger,
    )

    return WasmSqliteDriver(
        pathResolver = config.pathResolver,
        cApi = cApi,
        debugConfig = DebugConfigBlock().apply { config.debugConfigBlock(this) }.build(),
        rootLogger = config.logger,
        openParams = OpenParamsBlock().apply { config.openParams(this) },
    )
}
