/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm

import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.chasm.exports.ChasmEmscriptenMainExports
import ru.pixnews.wasm.sqlite.open.helper.chasm.exports.ChasmSqliteExports
import ru.pixnews.wasm.sqlite.open.helper.chasm.exports.ChicoryEmscriptenStackExports
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.ChasmInstanceBuilder
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.ChasmInstanceBuilder.ChasmInstance
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.EmbedderMemory
import ru.pixnews.wasm.sqlite.open.helper.embedder.SQLiteEmbedderRuntimeInfo
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteWasmEnvironment
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.exports.SqliteExports
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.EmscriptenRuntime
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.stack.EmscriptenStack
import java.util.concurrent.atomic.AtomicReference

public object ChasmSqliteEmbedder : SqliteEmbedder<ChasmSqliteEmbedderConfig> {
    @InternalWasmSqliteHelperApi
    override fun createSqliteWasmEnvironment(
        commonConfig: WasmSqliteCommonConfig,
        callbackStore: SqliteCallbackStore,
        embedderConfigBuilder: ChasmSqliteEmbedderConfig.() -> Unit,
    ): SqliteWasmEnvironment {
        val config = ChasmSqliteEmbedderConfig(commonConfig.logger).apply(embedderConfigBuilder)
        return createChasmSqliteWasmEnvironment(
            config.host,
            callbackStore,
            config.sqlite3Binary,
        )
    }

    private fun createChasmSqliteWasmEnvironment(
        host: EmbedderHost,
        @Suppress("UnusedParameter") callbackStore: SqliteCallbackStore,
        sqlite3Binary: WasmSqliteConfiguration,
    ): SqliteWasmEnvironment {
        require(!sqlite3Binary.requireThreads) {
            "The specified SQLite binary is compiled with threading support, which is not compatible with the " +
                    "Chasm WebAssembly runtime. Use a version of SQLite compiled without thread support."
        }

        val emscriptenStackRef: AtomicReference<EmscriptenStack> = AtomicReference()
        val chasmInstance: ChasmInstance = ChasmInstanceBuilder(
            host = host,
            callbackStore = callbackStore,
            minMemorySize = sqlite3Binary.wasmMinMemorySize,
            emscriptenStackRef = emscriptenStackRef::get,
        ).setupChasmInstance(sqlite3Binary)

        val emscriptenRuntime = EmscriptenRuntime.emscriptenSingleThreadedRuntime(
            mainExports = ChasmEmscriptenMainExports(chasmInstance),
            stackExports = ChicoryEmscriptenStackExports(chasmInstance),
        )
        emscriptenStackRef.set(emscriptenRuntime.stack)

        emscriptenRuntime.initRuntime(chasmInstance.memory)

        return object : SqliteWasmEnvironment {
            override val sqliteExports: SqliteExports = ChasmSqliteExports(chasmInstance)
            override val embedderInfo: SQLiteEmbedderRuntimeInfo = object : SQLiteEmbedderRuntimeInfo {
                override val supportMultithreading: Boolean = false
            }
            override val memory: EmbedderMemory = chasmInstance.memory
            override val callbackFunctionIndexes: Sqlite3CallbackFunctionIndexes =
                chasmInstance.indirectFunctionIndexes
        }
    }
}
