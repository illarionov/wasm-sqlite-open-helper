/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory

import at.released.weh.host.EmbedderHost
import at.released.weh.wasm.core.memory.Memory
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Machine
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.binary.reader.WasmSourceReader
import ru.pixnews.wasm.sqlite.open.helper.chicory.exports.ChicorySqliteExports
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.MainInstanceBuilder
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.MainInstanceBuilder.ChicoryInstance
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderRuntimeInfo
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteRuntimeInternal
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.exports.SqliteExports
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.SqliteCallbackFunctionIndexes

public object ChicorySqliteEmbedder : SqliteEmbedder<ChicorySqliteEmbedderConfig, ChicoryRuntime> {
    @InternalWasmSqliteHelperApi
    override fun createRuntime(
        commonConfig: WasmSqliteCommonConfig,
        embedderConfigBuilder: ChicorySqliteEmbedderConfig.() -> Unit,
    ): SqliteRuntimeInternal<ChicoryRuntime> {
        val config = mergeConfig(commonConfig, embedderConfigBuilder)
        return createChicorySqliteWasmEnvironment(
            config.host,
            config.sqlite3Binary,
            config.wasmSourceReader,
            config.machineFactory,
        )
    }

    internal fun mergeConfig(
        commonConfig: WasmSqliteCommonConfig,
        embedderConfigBuilder: ChicorySqliteEmbedderConfig.() -> Unit,
    ): ChicorySqliteEmbedderConfig = ChicorySqliteEmbedderConfig(
        rootLogger = commonConfig.logger,
        defaultWasmSourceReader = commonConfig.wasmReader,
    ).apply(embedderConfigBuilder)

    @Suppress("LAMBDA_IS_NOT_LAST_PARAMETER")
    private fun createChicorySqliteWasmEnvironment(
        host: EmbedderHost,
        sqlite3Binary: WasmSqliteConfiguration,
        wasmSourceReader: WasmSourceReader,
        machineFactory: ((Instance) -> Machine)?,
    ): SqliteRuntimeInternal<ChicoryRuntime> {
        require(!sqlite3Binary.requireThreads) {
            "The specified SQLite binary is compiled with threading support, which is not compatible with the " +
                    "Chicory WebAssembly runtime. Use a version of SQLite compiled without thread support."
        }
        val callbackStore = SqliteCallbackStore()

        val chicoryInstance: ChicoryInstance = MainInstanceBuilder(
            host = host,
            callbackStore = callbackStore,
            sqlite3Binary = sqlite3Binary,
            machineFactory = machineFactory,
            wasmSourceReader = wasmSourceReader,
        ).setupModule()

        val runtimeInstance = object : ChicoryRuntime {
            override val embedderInfo: SqliteEmbedderRuntimeInfo = object : SqliteEmbedderRuntimeInfo {
                override val supportMultithreading: Boolean = false
            }
        }

        return object : SqliteRuntimeInternal<ChicoryRuntime> {
            override val sqliteExports: SqliteExports = ChicorySqliteExports(chicoryInstance.instance)
            override val memory: Memory = chicoryInstance.memory
            override val callbackStore: SqliteCallbackStore = callbackStore
            override val callbackFunctionIndexes: SqliteCallbackFunctionIndexes =
                chicoryInstance.indirectFunctionIndexes
            override val runtimeInstance: ChicoryRuntime = runtimeInstance
            override fun close() = Unit
        }
    }
}
