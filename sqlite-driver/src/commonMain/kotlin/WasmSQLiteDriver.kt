/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:kotlin.jvm.JvmName("WasmSQLiteDriverBuilder")

package at.released.wasm.sqlite.driver

import androidx.sqlite.SQLiteDriver
import at.released.cassettes.playhead.AssetManager
import at.released.wasm.sqlite.driver.dsl.OpenParamsBlock
import at.released.wasm.sqlite.driver.dsl.WasmSqliteDriverConfigBlock
import at.released.wasm.sqlite.driver.internal.WasmSqliteDriverImpl
import at.released.wasm.sqlite.open.helper.debug.EmbedderHostLogger
import at.released.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfigBlock
import at.released.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import at.released.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import at.released.wasm.sqlite.open.helper.embedder.SqliteRuntime
import at.released.wasm.sqlite.open.helper.embedder.SqliteRuntimeInternal
import at.released.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import at.released.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3CApi
import at.released.weh.common.api.Logger

/**
 * Creates a [SQLiteDriver] with the specified [block] configuration.
 *
 * @param embedder WebAssembly Runtime (embedder) for running SQLite compiled to Wasm.
 * For example, GraalvmSqliteEmbedder
 */
public fun <E : SqliteEmbedderConfig, R : SqliteRuntime> WasmSQLiteDriver(
    embedder: SqliteEmbedder<E, R>,
    block: WasmSqliteDriverConfigBlock<E>.() -> Unit = {},
): WasmSQLiteDriver<R> = WasmSQLiteDriver(
    embedder = embedder,
    defaultWasmSourceReader = AssetManager,
    block = block,
)

internal fun <E : SqliteEmbedderConfig, R : SqliteRuntime> WasmSQLiteDriver(
    embedder: SqliteEmbedder<E, R>,
    defaultWasmSourceReader: AssetManager,
    block: WasmSqliteDriverConfigBlock<E>.() -> Unit = {},
): WasmSQLiteDriver<R> {
    val config = WasmSqliteDriverConfigBlock<E>().apply(block)
    val rootCommonConfig = object : WasmSqliteCommonConfig {
        override val logger: Logger = config.logger
        override val wasmReader: AssetManager = defaultWasmSourceReader
    }
    val debugConfig = WasmSqliteDebugConfigBlock().apply { config.debugConfigBlock(this) }.build(rootCommonConfig)
    val embedderLogger = debugConfig.getOrCreateDefault(EmbedderHostLogger)
    val embedderEnv = createEmbedder(embedder, embedderLogger, rootCommonConfig.wasmReader, config.embedderConfig)

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
        onClose = embedderEnv,
    )
}

private fun <E : SqliteEmbedderConfig, R : SqliteRuntime> createEmbedder(
    embedder: SqliteEmbedder<E, R>,
    embedderLogger: EmbedderHostLogger,
    embedderReader: AssetManager,
    embedderConfig: E.() -> Unit,
): SqliteRuntimeInternal<R> {
    val embedderCommonConfig = object : WasmSqliteCommonConfig {
        override val logger: Logger = if (embedderLogger.enabled) {
            embedderLogger.logger
        } else {
            Logger
        }
        override val wasmReader: AssetManager = embedderReader
    }

    return embedder.createRuntime(
        commonConfig = embedderCommonConfig,
        embedderConfigBuilder = embedderConfig,
    )
}

public interface WasmSQLiteDriver<R : SqliteRuntime> : AutoCloseable, SQLiteDriver {
    public val runtime: R

    public override fun close()
}
