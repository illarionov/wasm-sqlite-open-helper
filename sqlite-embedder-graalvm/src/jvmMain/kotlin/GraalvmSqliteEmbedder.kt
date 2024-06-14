/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.embedder.SQLiteEmbedderRuntimeInfo
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.graalvm.exports.GraalSqliteExports
import ru.pixnews.wasm.sqlite.open.helper.graalvm.exports.GraalvmEmscriptenMainExports
import ru.pixnews.wasm.sqlite.open.helper.graalvm.exports.GraalvmEmscriptenPthreadExports
import ru.pixnews.wasm.sqlite.open.helper.graalvm.exports.GraalvmEmscriptenStackExports
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory.GraalvmWasmHostMemoryAdapter
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.EmscriptenEnvModuleBuilder
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.sqlitecb.SqliteCallbacksModuleBuilder
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.wasi.WasiSnapshotPreview1ModuleBuilder
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.GraalvmManagedThreadFactory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.WasmManagedThreadStore
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.EmscriptenRuntime
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.PthreadManager
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.stack.EmscriptenStack
import java.net.URI
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicReference

public object GraalvmSqliteEmbedder : SqliteEmbedder<GraalvmSqliteEmbedderConfig, GraalvmRuntimeInstance> {
    private const val USE_UNSAFE_MEMORY: Boolean = false

    @InternalWasmSqliteHelperApi
    override fun createSqliteWasmEnvironment(
        commonConfig: WasmSqliteCommonConfig,
        embedderConfigBuilder: GraalvmSqliteEmbedderConfig.() -> Unit,
    ): GraalvmSqliteWasmEnvironment {
        val config = GraalvmSqliteEmbedderConfig(commonConfig.logger).apply(embedderConfigBuilder)
        return createGraalvmSqliteWasmEnvironment(
            config.graalvmEngine,
            config.host,
            config.sqlite3Binary,
        )
    }

    private fun createGraalvmSqliteWasmEnvironment(
        graalvmEngine: Engine,
        host: EmbedderHost,
        sqlite3Binary: WasmSqliteConfiguration,
    ): GraalvmSqliteWasmEnvironment {
        val useSharedMemory = USE_UNSAFE_MEMORY || sqlite3Binary.requireSharedMemory
        val wasmThreadsEnabled = sqlite3Binary.requireThreads
        val pthreadRef: AtomicReference<PthreadManager> = AtomicReference()
        val stackBindingsRef: AtomicReference<EmscriptenStack> = AtomicReference()

        val callbackStore = SqliteCallbackStore()
        val graalContext: Context = setupWasmGraalContext(graalvmEngine, useSharedMemory, wasmThreadsEnabled)
        val envModuleInstance = EmscriptenEnvModuleBuilder(
            graalContext,
            host,
            pthreadRef::get,
            stackBindingsRef::get,
        ).setupModule(
            minMemorySize = sqlite3Binary.wasmMinMemorySize,
            sharedMemory = sqlite3Binary.requireSharedMemory,
            useUnsafeMemory = useSharedMemory,
        )
        WasiSnapshotPreview1ModuleBuilder(graalContext, host).setupModule(
            sharedMemory = sqlite3Binary.requireSharedMemory,
            useUnsafeMemory = useSharedMemory,
        )

        val managedThreadStore = WasmManagedThreadStore()
        val sqliteCallbacksModuleBuilder = SqliteCallbacksModuleBuilder(
            graalContext = graalContext,
            host = host,
            callbackStore = callbackStore,
            managedThreadStore = managedThreadStore,
        )
        sqliteCallbacksModuleBuilder.setupModule(
            sharedMemory = sqlite3Binary.requireSharedMemory,
            useUnsafeMemory = useSharedMemory,
        )

        val sourceName = "sqlite3"
        val sqliteSource = Source.newBuilder("wasm", URI(sqlite3Binary.sqliteUrl).toURL())
            .name(sourceName)
            .build()
        graalContext.eval(sqliteSource)

        val indirectFunctionIndexes = sqliteCallbacksModuleBuilder.setupIndirectFunctionTable()

        val mainBindings = graalContext.getBindings("wasm").getMember(sourceName)
        val emscriptenRuntime = createEmscriptenRuntime(
            mainBindings = mainBindings,
            logger = host.rootLogger,
        )
        stackBindingsRef.set(emscriptenRuntime.stack)
        pthreadRef.set(emscriptenRuntime.pthreadManager)

        val memory = GraalvmWasmHostMemoryAdapter(
            envModuleInstance,
            null,
            host.fileSystem,
            host.rootLogger,
        )

        val sqliteExports = GraalSqliteExports(mainBindings)

        val runtimeInstance = object : GraalvmRuntimeInstance {
            override val embedderInfo: SQLiteEmbedderRuntimeInfo = GraalvmEmbedderInfo(
                wasmThreadsEnabled = sqlite3Binary.requireThreads,
            )
            override val managedThreadFactory: ThreadFactory = GraalvmManagedThreadFactory(
                threadManager = emscriptenRuntime.pthreadManager!!,
                managedThreadStore = managedThreadStore,
                dynamicMemory = sqliteExports.memoryExports,
                memory = memory,
                indirectFunctionIndexes = indirectFunctionIndexes,
                rootLogger = host.rootLogger,
            )
        }

        emscriptenRuntime.initRuntimeMainThread(memory)

        return GraalvmSqliteWasmEnvironment(
            sqliteExports = sqliteExports,
            memory = memory,
            callbackStore = callbackStore,
            callbackFunctionIndexes = indirectFunctionIndexes,
            runtimeInstance = runtimeInstance,
        )
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

    @Suppress("DEPRECATION")
    private fun createEmscriptenRuntime(
        mainBindings: Value,
        logger: Logger,
    ): EmscriptenRuntime {
        val currentThreadId = Thread.currentThread().id
        return EmscriptenRuntime.emscriptenMultithreadedRuntime(
            mainExports = GraalvmEmscriptenMainExports(mainBindings),
            stackExports = GraalvmEmscriptenStackExports(mainBindings),
            pthreadExports = GraalvmEmscriptenPthreadExports(mainBindings),
            logger = logger,
            isMainThread = { Thread.currentThread().id == currentThreadId },
        )
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
