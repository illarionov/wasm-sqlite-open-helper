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
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.EmbedderMemory
import ru.pixnews.wasm.sqlite.open.helper.embedder.SQLiteEmbedderRuntimeInfo
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteWasmEnvironment
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.graalvm.bindings.EmscriptenPthreadBindings
import ru.pixnews.wasm.sqlite.open.helper.graalvm.bindings.GraalSqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory.GraalvmWasmHostMemoryAdapter
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.EmscriptenEnvModuleBuilder
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.sqlitecb.SqliteCallbacksModuleBuilder
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.wasi.WasiSnapshotPreview1ModuleBuilder
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.pthread.Pthread
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import java.util.concurrent.atomic.AtomicReference

public object GraalvmSqliteEmbedder : SqliteEmbedder<GraalvmSqliteEmbedderConfig> {
    private const val USE_UNSAFE_MEMORY: Boolean = false

    @InternalWasmSqliteHelperApi
    override fun createSqliteWasmEnvironment(
        commonConfig: WasmSqliteCommonConfig,
        callbackStore: SqliteCallbackStore,
        embedderConfigBuilder: GraalvmSqliteEmbedderConfig.() -> Unit,
    ): SqliteWasmEnvironment {
        val config = GraalvmSqliteEmbedderConfig(commonConfig.logger).apply(embedderConfigBuilder)
        return createGraalvmSqliteWasmEnvironment(
            config.graalvmEngine,
            config.host,
            callbackStore,
            config.sqlite3Binary,
        )
    }

    private fun createGraalvmSqliteWasmEnvironment(
        graalvmEngine: Engine,
        host: EmbedderHost,
        callbackStore: SqliteCallbackStore,
        sqlite3Binary: WasmSqliteConfiguration,
    ): SqliteWasmEnvironment {
        val useSharedMemory = USE_UNSAFE_MEMORY || sqlite3Binary.requireSharedMemory
        val wasmThreadsEnabled = sqlite3Binary.requireThreads
        val graalContext: Context = setupWasmGraalContext(graalvmEngine, useSharedMemory, wasmThreadsEnabled)
        val ptreadRef: AtomicReference<Pthread> = AtomicReference()

        val sqliteCallbacksModuleBuilder = SqliteCallbacksModuleBuilder(graalContext, host, callbackStore)
        val envModuleInstance = EmscriptenEnvModuleBuilder(
            graalContext,
            host,
            ptreadRef::get,
        ).setupModule(
            minMemorySize = sqlite3Binary.wasmMinMemorySize,
            sharedMemory = sqlite3Binary.requireSharedMemory,
            useUnsafeMemory = useSharedMemory,
        )
        WasiSnapshotPreview1ModuleBuilder(graalContext, host).setupModule(
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

        val memory = GraalvmWasmHostMemoryAdapter(envModuleInstance, null, host.fileSystem, host.rootLogger)

        val bindings = GraalSqliteBindings(
            sqliteBindings = mainBindings,
            memory = memory,
        )
        return object : SqliteWasmEnvironment {
            override val sqliteBindings: SqliteBindings = bindings
            override val embedderInfo: SQLiteEmbedderRuntimeInfo = GraalvmEmbedderInfo(
                wasmThreadsEnabled = sqlite3Binary.requireThreads,
            )
            override val memory: EmbedderMemory = memory
            override val callbackFunctionIndexes: Sqlite3CallbackFunctionIndexes = indirectFunctionIndexes
        }
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

    private class GraalvmEmbedderInfo private constructor(
        override val supportMultithreading: Boolean,
    ) : SQLiteEmbedderRuntimeInfo {
        companion object {
            operator fun invoke(
                wasmThreadsEnabled: Boolean,
            ): GraalvmEmbedderInfo = GraalvmEmbedderInfo(supportMultithreading = wasmThreadsEnabled)
        }
    }
}
