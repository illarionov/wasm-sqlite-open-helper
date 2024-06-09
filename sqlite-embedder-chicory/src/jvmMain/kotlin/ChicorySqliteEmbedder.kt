/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory

import com.dylibso.chicory.log.Logger.Level
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.chicory.exports.ChicoryEmscriptenMainExports
import ru.pixnews.wasm.sqlite.open.helper.chicory.exports.ChicoryEmscriptenStackExports
import ru.pixnews.wasm.sqlite.open.helper.chicory.exports.ChicorySqliteExports
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.ChicoryLogger
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.MainInstanceBuilder
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.MainInstanceBuilder.ChicoryInstance
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
            config.logSeverity,
        )
    }

    private fun createChicorySqliteWasmEnvironment(
        host: EmbedderHost,
        callbackStore: SqliteCallbackStore,
        sqlite3Binary: WasmSqliteConfiguration,
        logSeverity: Level,
    ): SqliteWasmEnvironment {
        require(!sqlite3Binary.requireThreads) {
            "The specified SQLite binary is compiled with threading support, which is not compatible with the " +
                    "Chicory WebAssembly runtime. Use a version of SQLite compiled without thread support."
        }

        val chicoryLogger = ChicoryLogger(
            logger = host.rootLogger,
            severity = logSeverity,
        )
        val stackBindingsRef: AtomicReference<EmscriptenStack> = AtomicReference()
        val chicoryInstance: ChicoryInstance = MainInstanceBuilder(
            host = host,
            callbackStore = callbackStore,
            chicoryLogger = chicoryLogger,
            stackBindingsRef = stackBindingsRef::get,
            minMemorySize = sqlite3Binary.wasmMinMemorySize,
        ).setupModule(sqlite3Binary)

        val emscriptenRuntime = EmscriptenRuntime.emscriptenSingleThreadedRuntime(
            mainExports = ChicoryEmscriptenMainExports(chicoryInstance.instance),
            stackExports = ChicoryEmscriptenStackExports(chicoryInstance.instance),
        )
        stackBindingsRef.set(emscriptenRuntime.stack)

        emscriptenRuntime.initRuntime(chicoryInstance.memory)

        return object : SqliteWasmEnvironment {
            override val sqliteExports: SqliteExports = ChicorySqliteExports(chicoryInstance.instance)
            override val embedderInfo: SQLiteEmbedderRuntimeInfo = object : SQLiteEmbedderRuntimeInfo {
                override val supportMultithreading: Boolean = false
            }
            override val memory: EmbedderMemory = chicoryInstance.memory
            override val callbackFunctionIndexes: Sqlite3CallbackFunctionIndexes =
                chicoryInstance.indirectFunctionIndexes
        }
    }
}
