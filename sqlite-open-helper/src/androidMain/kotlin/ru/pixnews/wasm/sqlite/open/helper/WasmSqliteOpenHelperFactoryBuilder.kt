/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("FunctionName")

package ru.pixnews.wasm.sqlite.open.helper

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.dsl.DebugConfigBlock
import ru.pixnews.wasm.sqlite.open.helper.dsl.OpenParamsBlock
import ru.pixnews.wasm.sqlite.open.helper.dsl.WasmSqliteOpenHelperFactoryConfigBlock
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteWasmEnvironment
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import ru.pixnews.wasm.sqlite.open.helper.internal.CloseGuard
import ru.pixnews.wasm.sqlite.open.helper.internal.CloseGuard.Reporter
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.JvmSqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.path.AndroidDatabasePathResolver
import ru.pixnews.wasm.sqlite.open.helper.path.DatabasePathResolver
import ru.pixnews.wasm.sqlite.open.helper.path.JvmDatabasePathResolver

/**
 * Creates a [SupportSQLiteOpenHelper.Factory] with the specified [block] configuration.
 *
 * @param context Application context
 * @param embedder WebAssembly Runtime (embedder) for running SQLite compiled to Wasm.
 * For example, GraalvmSqliteEmbedder
 *
 */
public fun <E : SqliteEmbedderConfig> WasmSqliteOpenHelperFactory(
    embedder: SqliteEmbedder<E>,
    context: Context,
    block: WasmSqliteOpenHelperFactoryConfigBlock<E>.() -> Unit,
): SupportSQLiteOpenHelper.Factory = WasmSqliteOpenHelperFactory(
    embedder = embedder,
    defaultPathResolver = AndroidDatabasePathResolver(context),
    block = block,
)

/**
 * Creates a [SupportSQLiteOpenHelper.Factory] with the specified [block] configuration.
 *
 * @param embedder WebAssembly Runtime (embedder) for running SQLite compiled to Wasm.
 * For example, GraalvmSqliteEmbedder
 */
public fun <E : SqliteEmbedderConfig> WasmSqliteOpenHelperFactory(
    embedder: SqliteEmbedder<E>,
    block: WasmSqliteOpenHelperFactoryConfigBlock<E>.() -> Unit,
): SupportSQLiteOpenHelper.Factory = WasmSqliteOpenHelperFactory(
    embedder = embedder,
    defaultPathResolver = JvmDatabasePathResolver(),
    block = block,
)

internal fun <E : SqliteEmbedderConfig> WasmSqliteOpenHelperFactory(
    embedder: SqliteEmbedder<E>,
    defaultPathResolver: DatabasePathResolver,
    block: WasmSqliteOpenHelperFactoryConfigBlock<E>.() -> Unit,
): SupportSQLiteOpenHelper.Factory {
    val config = WasmSqliteOpenHelperFactoryConfigBlock<E>(defaultPathResolver).apply(block)
    val commonConfig = object : WasmSqliteCommonConfig {
        override val logger: Logger = config.logger
    }

    setupCloseGuard(config.logger)

    val callbackStore = JvmSqliteCallbackStore()
    val embedderEnv: SqliteWasmEnvironment = embedder.createSqliteWasmEnvironment(
        commonConfig = commonConfig,
        callbackStore = callbackStore,
        embedderConfigBuilder = config.embedderConfig,
    )

    return WasmSqliteOpenHelperFactory(
        pathResolver = config.pathResolver,
        sqliteBindings = embedderEnv.sqliteBindings,
        embedderInfo =  embedderEnv.embedderInfo,
        memory = embedderEnv.memory,
        callbackStore = callbackStore,
        callbackFunctionIndexes = embedderEnv.callbackFunctionIndexes,
        debugConfig = DebugConfigBlock().apply { config.debugConfigBlock(this) }.build(),
        rootLogger = config.logger,
        openParams = OpenParamsBlock().apply { config.openParams(this) },
    )
}

private fun setupCloseGuard(rootLogger: Logger) {
    val logger = rootLogger.withTag("SQLite")
    CloseGuard.reporter = Reporter { message, allocationSite ->
        logger.w(allocationSite, message::toString)
    }
}
