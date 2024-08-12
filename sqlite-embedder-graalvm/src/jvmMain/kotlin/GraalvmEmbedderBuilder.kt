/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.io.ByteSequence
import org.graalvm.wasm.WasmInstance
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.binary.reader.WasmSourceReader
import ru.pixnews.wasm.sqlite.binary.reader.readBytesOrThrow
import ru.pixnews.wasm.sqlite.open.helper.embedder.SQLiteEmbedderRuntimeInfo
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory.SharedMemoryWaiterListStore
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.EmscriptenEnvModuleBuilder
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.sqlitecb.GraalvmSqliteCallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.sqlitecb.SqliteCallbacksModuleBuilder
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.wasi.WasiSnapshotPreview1ModuleBuilder
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.GraalvmPthreadManager
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.stack.EmscriptenStack
import java.net.URI
import java.util.concurrent.atomic.AtomicReference

internal class GraalvmEmbedderBuilder(
    private val graalvmEngine: Engine,
    private val host: EmbedderHost,
    private val sqlite3Binary: WasmSqliteConfiguration,
    private val wasmSourceReader: WasmSourceReader,
) {
    val logger = host.rootLogger
    val sqliteSource: Source by lazy(LazyThreadSafetyMode.NONE) {
        val rawUrl = sqlite3Binary.sqliteUrl.url
        val builder = if (rawUrl.startsWith("jar:")) {
            Source.newBuilder("wasm", URI(rawUrl).toURL())
        } else {
            val bytes = wasmSourceReader.readBytesOrThrow(sqlite3Binary.sqliteUrl)
            Source.newBuilder("wasm", ByteSequence.create(bytes), SQLITE3_SOURCE_NAME)
        }
        builder
            .name(SQLITE3_SOURCE_NAME)
            .build()
    }
    val useSharedMemory = USE_UNSAFE_MEMORY || sqlite3Binary.requireThreads
    val wasmThreadsEnabled = sqlite3Binary.requireThreads

    fun createEnvironment(): GraalvmSqliteWasmEnvironment {
        val callbackStore = SqliteCallbackStore()
        val memoryWaiters = SharedMemoryWaiterListStore()

        val pthreadRef: AtomicReference<GraalvmPthreadManager> = AtomicReference()
        val stackBindingsRef: AtomicReference<EmscriptenStack> = AtomicReference()

        val graalContext: Context = createGraalContext()
        val modules = setupModules(
            graalContext = graalContext,
            mainThreadEnv = null,
            callbackStore = callbackStore,
            pthreadRef = pthreadRef::get,
            stackBindingsRef = stackBindingsRef::get,
            memoryWaiters = memoryWaiters,
        )

        graalContext.eval(sqliteSource)

        val indirectFunctionIndexes = modules.afterSourceEvaluated()

        val env = GraalvmSqliteWasmEnvironment(
            mainThreadGraalContext = graalContext,
            envModuleInstance = modules.envModuleInstance,
            callbackFunctionIndexes = indirectFunctionIndexes,
            host = host,
            embedderInfo = GraalvmEmbedderInfo(wasmThreadsEnabled),
            memoryWaiters = memoryWaiters,
            callbackStore = callbackStore,
            embedderBuilder = this,
        )

        stackBindingsRef.set(env.emscriptenRuntime.stack)
        pthreadRef.set(env.pthreadManager)

        env.emscriptenRuntime.initMainThread()

        return env
    }

    fun initChildThreadContext(
        parent: GraalvmSqliteWasmEnvironment,
    ): Context {
        val callbackStore = parent.callbackStore
        val pthreadManagerRef = parent::pthreadManager
        val stackBindingsRef = parent.emscriptenRuntime::stack

        val childContext: Context = createGraalContext()
        val modules = setupModules(
            graalContext = childContext,
            mainThreadEnv = parent,
            callbackStore = callbackStore,
            pthreadRef = pthreadManagerRef,
            memoryWaiters = parent.memoryWaiters,
            stackBindingsRef = stackBindingsRef::get,
        )

        childContext.eval(sqliteSource)

        // XXX indirectFunctionIndexes should be thread-local?
        modules.afterSourceEvaluated()
        return childContext
    }

    private fun createGraalContext(): Context {
        val graalContext = Context.newBuilder("wasm")
            .engine(graalvmEngine)
            .allowAllAccess(true)
            .apply {
                if (useSharedMemory) {
                    this.option("wasm.UseUnsafeMemory", "true")
                }
                if (wasmThreadsEnabled) {
                    this.option("wasm.Threads", "true")
                    this.option("wasm.BulkMemoryAndRefTypes", "true")
                }
            }
            .build()
        graalContext.initialize("wasm")
        return graalContext
    }

    private fun setupModules(
        graalContext: Context,
        mainThreadEnv: GraalvmSqliteWasmEnvironment?,
        callbackStore: SqliteCallbackStore,
        memoryWaiters: SharedMemoryWaiterListStore,
        pthreadRef: () -> GraalvmPthreadManager,
        stackBindingsRef: () -> EmscriptenStack,
    ): WasmModuleInstances {
        val emscriptenModuleBuilder = EmscriptenEnvModuleBuilder(
            host = host,
            pthreadRef = pthreadRef,
            emscriptenStackRef = stackBindingsRef,
            memoryWaiters = memoryWaiters,
        )
        val envModuleInstance = if (mainThreadEnv == null) {
            emscriptenModuleBuilder.setupModule(
                graalContext = graalContext,
                minMemorySize = sqlite3Binary.wasmMinMemorySize,
                sharedMemory = sqlite3Binary.requireThreads,
                useUnsafeMemory = useSharedMemory,
            )
        } else {
            emscriptenModuleBuilder.setupChildThreadModule(
                mainThreadEnv = mainThreadEnv,
                childGraalContext = graalContext,
            )
        }

        WasiSnapshotPreview1ModuleBuilder(graalContext, host).setupModule(
            sharedMemory = sqlite3Binary.requireThreads,
            useUnsafeMemory = useSharedMemory,
        )

        val sqliteCallbacksModuleBuilder = SqliteCallbacksModuleBuilder(
            graalContext = graalContext,
            host = host,
            callbackStore = callbackStore,
        )
        sqliteCallbacksModuleBuilder.setupModule(
            sharedMemory = sqlite3Binary.requireThreads,
            useUnsafeMemory = useSharedMemory,
        )
        return WasmModuleInstances(
            envModuleInstance = envModuleInstance,
            afterSourceEvaluated = sqliteCallbacksModuleBuilder::setupIndirectFunctionTable,
        )
    }

    private class WasmModuleInstances(
        val envModuleInstance: WasmInstance,
        val afterSourceEvaluated: () -> GraalvmSqliteCallbackFunctionIndexes,
    )

    internal class GraalvmEmbedderInfo private constructor(
        override val supportMultithreading: Boolean,
    ) : SQLiteEmbedderRuntimeInfo {
        companion object {
            operator fun invoke(
                wasmThreadsEnabled: Boolean,
            ): GraalvmEmbedderInfo = GraalvmEmbedderInfo(supportMultithreading = wasmThreadsEnabled)
        }
    }

    internal companion object {
        private const val USE_UNSAFE_MEMORY: Boolean = false
        internal const val SQLITE3_SOURCE_NAME = "sqlite3"
    }
}
