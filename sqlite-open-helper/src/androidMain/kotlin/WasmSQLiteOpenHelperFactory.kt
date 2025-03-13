/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("FunctionName")

package at.released.wasm.sqlite.open.helper

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import at.released.cassettes.playhead.AndroidAssetsAssetManager
import at.released.cassettes.playhead.AssetManager
import at.released.cassettes.playhead.JvmResourcesAssetManager
import at.released.wasm.sqlite.open.helper.debug.EmbedderHostLogger
import at.released.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfigBlock
import at.released.wasm.sqlite.open.helper.dsl.OpenParamsBlock
import at.released.wasm.sqlite.open.helper.dsl.WasmSqliteOpenHelperFactoryConfigBlock
import at.released.wasm.sqlite.open.helper.dsl.path.AndroidDatabasePathResolver
import at.released.wasm.sqlite.open.helper.dsl.path.DatabasePathResolver
import at.released.wasm.sqlite.open.helper.dsl.path.JvmDatabasePathResolver
import at.released.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import at.released.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import at.released.wasm.sqlite.open.helper.embedder.SqliteRuntime
import at.released.wasm.sqlite.open.helper.embedder.SqliteRuntimeInternal
import at.released.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import at.released.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import at.released.wasm.sqlite.open.helper.internal.CloseGuard
import at.released.wasm.sqlite.open.helper.internal.CloseGuard.Reporter
import at.released.weh.common.api.Logger

/**
 * Creates a [SupportSQLiteOpenHelper.Factory] with the specified [block] configuration.
 *
 * @param context Application context
 * @param embedder WebAssembly Runtime (embedder) for running SQLite compiled to Wasm.
 * For example, GraalvmSqliteEmbedder
 *
 */
public fun <E : SqliteEmbedderConfig, R : SqliteRuntime> WasmSqliteOpenHelperFactory(
    embedder: SqliteEmbedder<E, R>,
    context: Context,
    block: WasmSqliteOpenHelperFactoryConfigBlock<E>.() -> Unit = {},
): WasmSQLiteOpenHelperFactory<R> = WasmSqliteOpenHelperFactory(
    embedder = embedder,
    defaultPathResolver = AndroidDatabasePathResolver(context),
    defaultWasmSourceReader = AndroidAssetsAssetManager(context.assets),
    block = block,
)

/**
 * Creates a [SupportSQLiteOpenHelper.Factory] with the specified [block] configuration.
 *
 * @param embedder WebAssembly Runtime (embedder) for running SQLite compiled to Wasm.
 * For example, GraalvmSqliteEmbedder
 */
public fun <E : SqliteEmbedderConfig, R : SqliteRuntime> WasmSqliteOpenHelperFactory(
    embedder: SqliteEmbedder<E, R>,
    block: WasmSqliteOpenHelperFactoryConfigBlock<E>.() -> Unit = {},
): WasmSQLiteOpenHelperFactory<R> = WasmSqliteOpenHelperFactory(
    embedder = embedder,
    defaultPathResolver = JvmDatabasePathResolver(),
    defaultWasmSourceReader = JvmResourcesAssetManager(),
    block = block,
)

internal fun <E : SqliteEmbedderConfig, R : SqliteRuntime> WasmSqliteOpenHelperFactory(
    embedder: SqliteEmbedder<E, R>,
    defaultPathResolver: DatabasePathResolver,
    defaultWasmSourceReader: AssetManager,
    block: WasmSqliteOpenHelperFactoryConfigBlock<E>.() -> Unit = {},
): WasmSQLiteOpenHelperFactory<R> {
    val config = WasmSqliteOpenHelperFactoryConfigBlock<E>(defaultPathResolver).apply(block)
    val rootCommonConfig = object : WasmSqliteCommonConfig {
        override val logger: Logger = config.logger
        override val wasmReader: AssetManager = defaultWasmSourceReader
    }
    val debugConfig = WasmSqliteDebugConfigBlock().apply { config.debugConfigBlock(this) }.build(rootCommonConfig)
    val embedderLogger = debugConfig.getOrCreateDefault(EmbedderHostLogger)
    val embedderEnv = createEmbedder(embedder, embedderLogger, defaultWasmSourceReader, config.embedderConfig)

    setupCloseGuard(config.logger)

    return WasmSqliteOpenHelperFactoryImpl(
        pathResolver = config.pathResolver,
        sqliteExports = embedderEnv.sqliteExports,
        runtime = embedderEnv.runtimeInstance,
        memory = embedderEnv.memory,
        callbackStore = SqliteCallbackStore(),
        callbackFunctionIndexes = embedderEnv.callbackFunctionIndexes,
        debugConfig = debugConfig,
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

private fun <E : SqliteEmbedderConfig, R : SqliteRuntime> createEmbedder(
    embedder: SqliteEmbedder<E, R>,
    embedderLogger: EmbedderHostLogger,
    embedderWasmReader: AssetManager,
    embedderConfig: E.() -> Unit,
): SqliteRuntimeInternal<R> {
    val embedderCommonConfig = object : WasmSqliteCommonConfig {
        override val logger: Logger = if (embedderLogger.enabled) {
            embedderLogger.logger
        } else {
            Logger
        }
        override val wasmReader: AssetManager = embedderWasmReader
    }

    return embedder.createRuntime(
        commonConfig = embedderCommonConfig,
        embedderConfigBuilder = embedderConfig,
    )
}

public interface WasmSQLiteOpenHelperFactory<R : SqliteRuntime> : SupportSQLiteOpenHelper.Factory {
    public val runtime: R
}
