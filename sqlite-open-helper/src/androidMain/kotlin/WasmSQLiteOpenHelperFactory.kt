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
import ru.pixnews.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfigBlock
import ru.pixnews.wasm.sqlite.open.helper.dsl.OpenParamsBlock
import ru.pixnews.wasm.sqlite.open.helper.dsl.WasmSqliteOpenHelperFactoryConfigBlock
import ru.pixnews.wasm.sqlite.open.helper.dsl.path.AndroidDatabasePathResolver
import ru.pixnews.wasm.sqlite.open.helper.dsl.path.DatabasePathResolver
import ru.pixnews.wasm.sqlite.open.helper.dsl.path.JvmDatabasePathResolver
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteRuntimeInstance
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteWasmEnvironment
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.internal.CloseGuard
import ru.pixnews.wasm.sqlite.open.helper.internal.CloseGuard.Reporter

/**
 * Creates a [SupportSQLiteOpenHelper.Factory] with the specified [block] configuration.
 *
 * @param context Application context
 * @param embedder WebAssembly Runtime (embedder) for running SQLite compiled to Wasm.
 * For example, GraalvmSqliteEmbedder
 *
 */
public fun <E : SqliteEmbedderConfig, R : SqliteRuntimeInstance> WasmSqliteOpenHelperFactory(
    embedder: SqliteEmbedder<E, R>,
    context: Context,
    block: WasmSqliteOpenHelperFactoryConfigBlock<E>.() -> Unit = {},
): WasmSQLiteOpenHelperFactory<R> = WasmSqliteOpenHelperFactory(
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
public fun <E : SqliteEmbedderConfig, R : SqliteRuntimeInstance> WasmSqliteOpenHelperFactory(
    embedder: SqliteEmbedder<E, R>,
    block: WasmSqliteOpenHelperFactoryConfigBlock<E>.() -> Unit = {},
): WasmSQLiteOpenHelperFactory<R> = WasmSqliteOpenHelperFactory(
    embedder = embedder,
    defaultPathResolver = JvmDatabasePathResolver(),
    block = block,
)

internal fun <E : SqliteEmbedderConfig, R : SqliteRuntimeInstance> WasmSqliteOpenHelperFactory(
    embedder: SqliteEmbedder<E, R>,
    defaultPathResolver: DatabasePathResolver,
    block: WasmSqliteOpenHelperFactoryConfigBlock<E>.() -> Unit = {},
): WasmSQLiteOpenHelperFactory<R> {
    val config = WasmSqliteOpenHelperFactoryConfigBlock<E>(defaultPathResolver).apply(block)
    val commonConfig = object : WasmSqliteCommonConfig {
        override val logger: Logger = config.logger
    }

    setupCloseGuard(config.logger)

    val callbackStore = SqliteCallbackStore()
    val embedderEnv: SqliteWasmEnvironment<R> = embedder.createSqliteWasmEnvironment(
        commonConfig = commonConfig,
        embedderConfigBuilder = config.embedderConfig,
    )

    return WasmSqliteOpenHelperFactoryImpl(
        pathResolver = config.pathResolver,
        sqliteExports = embedderEnv.sqliteExports,
        runtime = embedderEnv.runtimeInstance,
        memory = embedderEnv.memory,
        callbackStore = callbackStore,
        callbackFunctionIndexes = embedderEnv.callbackFunctionIndexes,
        debugConfig = WasmSqliteDebugConfigBlock().apply { config.debugConfigBlock(this) }.build(commonConfig),
        rootLogger = config.logger,
        openParams = OpenParamsBlock().apply { config.openParams(this) },
    )
}

public interface WasmSQLiteOpenHelperFactory<R : SqliteRuntimeInstance> : SupportSQLiteOpenHelper.Factory {
    public val runtime: R
}

private fun setupCloseGuard(rootLogger: Logger) {
    val logger = rootLogger.withTag("SQLite")
    CloseGuard.reporter = Reporter { message, allocationSite ->
        logger.w(allocationSite, message::toString)
    }
}
