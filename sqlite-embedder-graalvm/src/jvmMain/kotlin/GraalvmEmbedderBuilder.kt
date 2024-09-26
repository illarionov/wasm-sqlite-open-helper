/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm

import at.released.weh.bindings.graalvm241.GraalvmEmscriptenEnvironment
import at.released.weh.bindings.graalvm241.GraalvmHostFunctionInstaller
import at.released.weh.bindings.graalvm241.MemorySource.ExportedMemory
import at.released.weh.bindings.graalvm241.MemorySource.ImportedMemory
import at.released.weh.bindings.graalvm241.MemorySpec
import at.released.weh.bindings.graalvm241.host.pthread.ManagedThreadInitializer
import at.released.weh.emcripten.runtime.export.IndirectFunctionTableIndex
import at.released.weh.host.EmbedderHost
import at.released.weh.wasm.core.WasmModules.WASI_SNAPSHOT_PREVIEW1_MODULE_NAME
import at.released.weh.wasm.core.memory.Pages
import at.released.weh.wasm.core.memory.WASM_MEMORY_PAGE_SIZE
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.io.ByteSequence
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.binary.reader.WasmSourceReader
import ru.pixnews.wasm.sqlite.binary.reader.readBytesOrThrow
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderRuntimeInfo
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.sqlitecb.GraalvmSqliteCallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.sqlitecb.SqliteCallbacksModuleBuilder
import java.net.URI

internal class GraalvmEmbedderBuilder(
    private val graalvmEngine: Engine,
    private val host: EmbedderHost,
    private val sqlite3Binary: WasmSqliteConfiguration,
    private val wasmSourceReader: WasmSourceReader,
) {
    val logger = host.rootLogger
    private val sqliteSource: Source by lazy(LazyThreadSafetyMode.NONE) {
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
    private val wasmThreadsEnabled = sqlite3Binary.requireThreads
    private val memorySpec = MemorySpec {
        this.minSize = Pages(sqlite3Binary.wasmMinMemorySize / WASM_MEMORY_PAGE_SIZE)
        this.shared = sqlite3Binary.requireThreads
        this.useUnsafe = USE_UNSAFE_MEMORY || sqlite3Binary.requireThreads
    }
    private val sqliteCallbacksStore = SqliteCallbackStore()

    fun createEnvironment(): GraalvmSqliteRuntimeInternal {
        val graalContext: Context = createGraalContext()
        val installer = GraalvmHostFunctionInstaller(graalContext) {
            this.host = this@GraalvmEmbedderBuilder.host
        }

        installer.setupWasiPreview1Module(
            moduleName = WASI_SNAPSHOT_PREVIEW1_MODULE_NAME,
            memory = ImportedMemory(
                moduleName = "env",
                memoryName = "memory",
                spec = memorySpec,
            ),
        )

        val envInstaller = installer.setupEmscriptenFunctions(
            moduleName = "env",
            memory = ExportedMemory(spec = memorySpec),
        )

        val callbacksModuleInstaller = setupCallbacksModule(graalContext, sqliteCallbacksStore)

        graalContext.eval(sqliteSource)

        val indirectFunctionIndexes = callbacksModuleInstaller.afterSourceEvaluated()

        val graalInternalEnvironment: GraalvmEmscriptenEnvironment = envInstaller.finalize(
            mainModuleName = SQLITE3_SOURCE_NAME,
        ).apply {
            externalManagedThreadStartRoutine = IndirectFunctionTableIndex(
                indirectFunctionIndexes.externalManagedThreadStartRoutine.funcId,
            )
            managedThreadInitializer = object : ManagedThreadInitializer {
                override fun destroyThreadLocalGraalvmAgent() {
                    logger.v { "destroyThreadLocalGraalvmAgent()" }
                    threadLocalGraalContext.remove()
                    // Does not close the context in the dying thread, as this leads to the closing of shared memory
                    // used in other threads
                    // context?.close()
                }

                override fun initThreadLocalGraalvmAgent() {
                    val newContext = initWorkerThreadContext(this@apply)
                    threadLocalGraalContext.apply {
                        check(get() == null) {
                            "Graalvm agent already initialized in this thread"
                        }
                        set(newContext)
                    }
                }

                override fun initWorkerThread(threadPtr: Int) {
                    emscriptenRuntime.initWorkerThread(threadPtr)
                }
            }
        }

        graalInternalEnvironment.emscriptenRuntime.initMainThread()

        return GraalvmSqliteRuntimeInternal(
            mainModuleName = SQLITE3_SOURCE_NAME,
            rootContext = graalContext,
            embedderInfo = GraalvmEmbedderInfo(wasmThreadsEnabled),
            emscriptenEnvironment = graalInternalEnvironment,
            callbackStore = sqliteCallbacksStore,
            callbackFunctionIndexes = indirectFunctionIndexes,
        )
    }

    fun initWorkerThreadContext(
        parentEnv: GraalvmEmscriptenEnvironment,
    ): Context {
        val workerContext: Context = createGraalContext()
        parentEnv.getWorkerThreadInstaller(workerContext).apply {
            setupWasiPreview1Module(
                moduleName = WASI_SNAPSHOT_PREVIEW1_MODULE_NAME,
                memory = ImportedMemory(
                    moduleName = "env",
                    memoryName = "memory",
                    spec = memorySpec,
                ),
            )

            setupEmscriptenFunctions()
        }

        val callbacksModuleInstaller = setupCallbacksModule(workerContext, sqliteCallbacksStore)

        workerContext.eval(sqliteSource)

        // XXX indirectFunctionIndexes should be thread-local?
        callbacksModuleInstaller.afterSourceEvaluated()

        return workerContext
    }

    private fun createGraalContext(): Context {
        val graalContext = Context.newBuilder("wasm")
            .engine(graalvmEngine)
            .allowAllAccess(true)
            .apply {
                if (memorySpec.useUnsafeMemory) {
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

    private fun setupCallbacksModule(
        graalContext: Context,
        callbackStore: SqliteCallbackStore,
    ): WasmModuleInstances {
        val sqliteCallbacksModuleBuilder = SqliteCallbacksModuleBuilder(
            graalContext = graalContext,
            host = host,
            callbackStore = callbackStore,
        )
        sqliteCallbacksModuleBuilder.setupModule(
            sharedMemory = sqlite3Binary.requireThreads,
            useUnsafeMemory = memorySpec.useUnsafeMemory,
        )
        return WasmModuleInstances(
            afterSourceEvaluated = sqliteCallbacksModuleBuilder::setupIndirectFunctionTable,
        )
    }

    private class WasmModuleInstances(
        val afterSourceEvaluated: () -> GraalvmSqliteCallbackFunctionIndexes,
    )

    internal class GraalvmEmbedderInfo private constructor(
        override val supportMultithreading: Boolean,
    ) : SqliteEmbedderRuntimeInfo {
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
