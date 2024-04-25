/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Module
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.chicory.bindings.ChicorySqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.EnvModuleBuilder
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.EnvModuleBuilder.EnvModule
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.EmbedderMemory
import ru.pixnews.wasm.sqlite.open.helper.embedder.SQLiteEmbedderRuntimeInfo
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteWasmEnvironment
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes

public object ChicorySqliteEmbedder : SqliteEmbedder<ChicorySqliteEmbedderConfig> {
    @InternalWasmSqliteHelperApi
    override fun createSqliteWasmEnvironment(
        commonConfig: WasmSqliteCommonConfig,
        callbackStore: SqliteCallbackStore,
        embedderConfigBuilder: ChicorySqliteEmbedderConfig.() -> Unit,
    ): SqliteWasmEnvironment {
        val config = ChicorySqliteEmbedderConfig(commonConfig.logger).apply(embedderConfigBuilder)
        return createChicorySqliteWasmEnvironment(
            config.host,
            callbackStore,
            config.sqlite3Binary,
        )
    }

    private fun createChicorySqliteWasmEnvironment(
        host: SqliteEmbedderHost,
        @Suppress("UnusedParameter") callbackStore: SqliteCallbackStore,
        sqlite3Binary: WasmSqliteConfiguration,
    ): SqliteWasmEnvironment {
        require(!sqlite3Binary.requireThreads) {
            "The specified SQLite binary is compiled with threading support, which is not compatible with the " +
                    "Chicory WebAssembly runtime. Use a version of SQLite compiled without thread support."
        }

        val sqlite3Module = sqlite3Binary.sqliteUrl.openStream().use {
            Module.builder(it).build()
        }
        val envModule: EnvModule = EnvModuleBuilder(
            host = host,
            minMemorySize = sqlite3Binary.wasmMinMemorySize,
        ).setupModule()

        val instance: Instance = sqlite3Module.instantiate(envModule.hostImports)

        val bindings = ChicorySqliteBindings(envModule.memory, instance)

        return object : SqliteWasmEnvironment {
            override val sqliteBindings: SqliteBindings = bindings
            override val embedderInfo: SQLiteEmbedderRuntimeInfo = object : SQLiteEmbedderRuntimeInfo {
                override val supportMultithreading: Boolean = false
            }
            override val memory: EmbedderMemory = envModule.memory
            override val callbackFunctionIndexes: Sqlite3CallbackFunctionIndexes = envModule.indirectFunctionIndexes
        }
    }
}
