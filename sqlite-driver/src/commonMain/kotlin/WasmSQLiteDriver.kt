/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:kotlin.jvm.JvmName("WasmSQLiteDriverBuilder")

package ru.pixnews.wasm.sqlite.driver

import androidx.sqlite.SQLiteDriver
import ru.pixnews.wasm.sqlite.driver.dsl.OpenParamsBlock
import ru.pixnews.wasm.sqlite.driver.dsl.WasmSqliteDriverConfigBlock
import ru.pixnews.wasm.sqlite.driver.internal.WasmSqliteDriverImpl
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.debug.EmbedderHostLogger
import ru.pixnews.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfigBlock
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteRuntimeInstance
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteWasmEnvironment
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3CApi

/**
 * Creates a [SQLiteDriver] with the specified [block] configuration.
 *
 * @param embedder WebAssembly Runtime (embedder) for running SQLite compiled to Wasm.
 * For example, GraalvmSqliteEmbedder
 */
public fun <E : SqliteEmbedderConfig, R : SqliteRuntimeInstance> WasmSQLiteDriver(
    embedder: SqliteEmbedder<E, R>,
    block: WasmSqliteDriverConfigBlock<E>.() -> Unit = {},
): WasmSQLiteDriver<R> {
    val config = WasmSqliteDriverConfigBlock<E>().apply(block)
    val rootCommonConfig = object : WasmSqliteCommonConfig {
        override val logger: Logger = config.logger
    }
    val debugConfig = WasmSqliteDebugConfigBlock().apply { config.debugConfigBlock(this) }.build(rootCommonConfig)
    val embedderLogger = debugConfig.getOrCreateDefault(EmbedderHostLogger)
    val embedderEnv = createEmbedder(embedder, embedderLogger, config.embedderConfig)

    val cApi = Sqlite3CApi(
        sqliteExports = embedderEnv.sqliteExports,
        embedderInfo = embedderEnv.runtimeInstance.embedderInfo,
        memory = embedderEnv.memory,
        callbackStore = embedderEnv.callbackStore,
        callbackFunctionIndexes = embedderEnv.callbackFunctionIndexes,
        rootLogger = config.logger,
    )

    return WasmSqliteDriverImpl(
        cApi = cApi,
        debugConfig = debugConfig,
        rootLogger = config.logger,
        openParams = OpenParamsBlock().apply { config.openParams(this) },
        runtime = embedderEnv.runtimeInstance,
    )
}

private fun <E : SqliteEmbedderConfig, R : SqliteRuntimeInstance> createEmbedder(
    embedder: SqliteEmbedder<E, R>,
    embedderLogger: EmbedderHostLogger,
    embedderConfig: E.() -> Unit,
): SqliteWasmEnvironment<R> {
    val embedderCommonConfig = object : WasmSqliteCommonConfig {
        override val logger: Logger = if (embedderLogger.enabled) {
            embedderLogger.logger
        } else {
            Logger
        }
    }

    return embedder.createSqliteWasmEnvironment(
        commonConfig = embedderCommonConfig,
        embedderConfigBuilder = embedderConfig,
    )
}

public interface WasmSQLiteDriver<R : SqliteRuntimeInstance> : SQLiteDriver {
    public val runtime: R
}
