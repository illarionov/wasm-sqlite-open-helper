/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.chicory

import at.released.cassettes.playhead.AssetManager
import at.released.wasm.sqlite.binary.base.WasmSqliteConfiguration
import at.released.wasm.sqlite.open.helper.InternalWasmSqliteHelperApi
import at.released.wasm.sqlite.open.helper.chicory.exports.ChicorySqliteExports
import at.released.wasm.sqlite.open.helper.chicory.host.module.MainInstanceBuilder
import at.released.wasm.sqlite.open.helper.chicory.host.module.MainInstanceBuilder.ChicoryInstance
import at.released.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import at.released.wasm.sqlite.open.helper.embedder.SqliteEmbedderRuntimeInfo
import at.released.wasm.sqlite.open.helper.embedder.SqliteRuntimeInternal
import at.released.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import at.released.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import at.released.wasm.sqlite.open.helper.embedder.exports.SqliteExports
import at.released.wasm.sqlite.open.helper.embedder.functiontable.SqliteCallbackFunctionIndexes
import at.released.weh.host.EmbedderHost
import at.released.weh.wasm.core.memory.Memory
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Machine
import com.dylibso.chicory.wasm.types.MemoryLimits
import com.dylibso.chicory.runtime.Memory as ChicoryMemory

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
            config.memoryFactory,
        )
    }

    internal fun mergeConfig(
        commonConfig: WasmSqliteCommonConfig,
        embedderConfigBuilder: ChicorySqliteEmbedderConfig.() -> Unit,
    ): ChicorySqliteEmbedderConfig = ChicorySqliteEmbedderConfig(
        rootLogger = commonConfig.logger,
        defaultWasmSourceReader = commonConfig.wasmReader,
    ).apply(embedderConfigBuilder)

    private fun createChicorySqliteWasmEnvironment(
        host: EmbedderHost,
        sqlite3Binary: WasmSqliteConfiguration,
        wasmSourceReader: AssetManager,
        machineFactory: ((Instance) -> Machine)?,
        memoryFactory: ((MemoryLimits) -> ChicoryMemory)?,
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
            wasmSourceReader = wasmSourceReader,
            machineFactory = machineFactory,
            memoryFactory = memoryFactory,
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
