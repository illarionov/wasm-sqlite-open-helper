/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm

import at.released.weh.host.EmbedderHost
import at.released.weh.wasm.core.memory.Memory
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.binary.reader.WasmSourceReader
import ru.pixnews.wasm.sqlite.open.helper.chasm.exports.ChasmSqliteExports
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.ChasmInstanceBuilder
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.ChasmInstanceBuilder.ChasmInstance
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderRuntimeInfo
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteRuntimeInternal
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.exports.SqliteExports
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.SqliteCallbackFunctionIndexes

public object ChasmSqliteEmbedder : SqliteEmbedder<ChasmSqliteEmbedderConfig, ChasmRuntime> {
    @InternalWasmSqliteHelperApi
    override fun createRuntime(
        commonConfig: WasmSqliteCommonConfig,
        embedderConfigBuilder: ChasmSqliteEmbedderConfig.() -> Unit,
    ): SqliteRuntimeInternal<ChasmRuntime> {
        val config = mergeConfig(commonConfig, embedderConfigBuilder)
        return createChasmSqliteWasmEnvironment(
            config.host,
            config.sqlite3Binary,
            config.wasmSourceReader,
        )
    }

    internal fun mergeConfig(
        commonConfig: WasmSqliteCommonConfig,
        embedderConfigBuilder: ChasmSqliteEmbedderConfig.() -> Unit,
    ): ChasmSqliteEmbedderConfig {
        return ChasmSqliteEmbedderConfig(
            rootLogger = commonConfig.logger,
            defaultWasmSourceReader = commonConfig.wasmReader,
        ).apply(embedderConfigBuilder)
    }

    private fun createChasmSqliteWasmEnvironment(
        host: EmbedderHost,
        sqlite3Binary: WasmSqliteConfiguration,
        sourceReader: WasmSourceReader,
    ): SqliteRuntimeInternal<ChasmRuntime> {
        require(!sqlite3Binary.requireThreads) {
            "The specified SQLite binary is compiled with threading support, which is not compatible with the " +
                    "Chasm WebAssembly runtime. Use a version of SQLite compiled without thread support."
        }

        val callbackStore = SqliteCallbackStore()
        val chasmInstance: ChasmInstance = ChasmInstanceBuilder(
            host = host,
            callbackStore = callbackStore,
            sqlite3Binary = sqlite3Binary,
            sourceReader = sourceReader,
        ).setupChasmInstance()

        val runtimeInstance = object : ChasmRuntime {
            override val embedderInfo: SqliteEmbedderRuntimeInfo = object : SqliteEmbedderRuntimeInfo {
                override val supportMultithreading: Boolean = false
            }
        }

        return object : SqliteRuntimeInternal<ChasmRuntime> {
            override val sqliteExports: SqliteExports = ChasmSqliteExports(chasmInstance)
            override val memory: Memory = chasmInstance.memory
            override val callbackStore: SqliteCallbackStore = callbackStore
            override val callbackFunctionIndexes: SqliteCallbackFunctionIndexes =
                chasmInstance.indirectFunctionIndexes
            override val runtimeInstance: ChasmRuntime = runtimeInstance
            override fun close() = Unit
        }
    }
}
