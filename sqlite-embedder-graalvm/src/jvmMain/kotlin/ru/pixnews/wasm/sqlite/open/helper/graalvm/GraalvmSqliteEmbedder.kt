/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Source
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteCapi
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import ru.pixnews.wasm.sqlite.open.helper.graalvm.bindings.EmscriptenPthreadBindings
import ru.pixnews.wasm.sqlite.open.helper.graalvm.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.EmscriptenEnvModuleBuilder
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory.GraalvmWasmHostMemoryAdapter
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.preview1.WasiSnapshotPreview1MobuleBuilder
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.pthread.Pthread
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.GraalvmSqliteCapi
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.Sqlite3CallbackStore
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.SqliteCallbacksModuleBuilder
import java.util.concurrent.atomic.AtomicReference

public object GraalvmSqliteEmbedder : SqliteEmbedder<GraalvmSqliteEmbedderConfig> {
    private const val USE_UNSAFE_MEMORY: Boolean = false

    override fun createCapi(
        commonConfig: WasmSqliteCommonConfig,
        embedderConfigBuilder: GraalvmSqliteEmbedderConfig.() -> Unit,
    ): SqliteCapi {
        val config = GraalvmSqliteEmbedderConfig(commonConfig.logger).apply(embedderConfigBuilder)
        return createGraalvmSqliteCapi(
            config.graalvmEngine,
            config.host,
            config.sqlite3Binary,
        )
    }

    private fun createGraalvmSqliteCapi(
        graalvmEngine: Engine,
        host: SqliteEmbedderHost,
        sqlite3Binary: WasmSqliteConfiguration,
    ): SqliteCapi {
        val useSharedMemory = USE_UNSAFE_MEMORY || sqlite3Binary.requireSharedMemory
        val callbackStore = Sqlite3CallbackStore()
        val graalContext: Context = setupWasmGraalContext(graalvmEngine, useSharedMemory, sqlite3Binary.requireThreads)
        val ptreadRef: AtomicReference<Pthread> = AtomicReference()

        val sqliteCallbacksModuleBuilder = SqliteCallbacksModuleBuilder(graalContext, host, callbackStore)
        val envModuleInstance = EmscriptenEnvModuleBuilder(graalContext, host, ptreadRef::get).setupModule(
            minMemorySize = sqlite3Binary.wasmMinMemorySize,
            sharedMemory = sqlite3Binary.requireSharedMemory,
            useUnsafeMemory = useSharedMemory,
        )
        WasiSnapshotPreview1MobuleBuilder(graalContext, host).setupModule(
            sharedMemory = sqlite3Binary.requireSharedMemory,
            useUnsafeMemory = useSharedMemory,
        )
        sqliteCallbacksModuleBuilder.setupModule(
            sharedMemory = sqlite3Binary.requireSharedMemory,
            useUnsafeMemory = useSharedMemory,
        )

        val sourceName = "sqlite3"
        val sqliteSource = Source.newBuilder("wasm", sqlite3Binary.sqliteUrl)
            .name(sourceName)
            .build()
        graalContext.eval(sqliteSource)

        val indirectFunctionIndexes = sqliteCallbacksModuleBuilder.setupIndirectFunctionTable()

        val wasmBindings = graalContext.getBindings("wasm")
        val mainBindings = wasmBindings.getMember(sourceName)
        val pthreadBindings = EmscriptenPthreadBindings(graalContext, mainBindings)
        @Suppress("DEPRECATION")
        ptreadRef.set(
            Pthread(
                host.rootLogger,
                Thread.currentThread().id,
                pthreadBindings,
            ),
        )

        val memory = GraalvmWasmHostMemoryAdapter(envModuleInstance, null, host.rootLogger)

        val bindings = SqliteBindings(
            sqliteBindings = mainBindings,
            memory = memory,
        )
        return GraalvmSqliteCapi(bindings, memory, callbackStore, indirectFunctionIndexes, host.rootLogger)
    }

    private fun setupWasmGraalContext(
        graalvmEngine: Engine,
        useSharedMemory: Boolean,
        requireThreads: Boolean,
    ): Context {
        val graalContext = Context.newBuilder("wasm")
            .engine(graalvmEngine)
            .allowAllAccess(true)
            .apply {
                if (useSharedMemory) {
                    this.option("wasm.UseUnsafeMemory", "true")
                }
                if (requireThreads) {
                    this.option("wasm.Threads", "true")
                    this.option("wasm.BulkMemoryAndRefTypes", "true")
                }
            }
            .build()
        graalContext.initialize("wasm")
        return graalContext
    }
}
