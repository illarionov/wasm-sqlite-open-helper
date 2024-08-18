/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import org.graalvm.wasm.WasmInstance
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderRuntimeInfo
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteRuntimeInternal
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.exports.SqliteExports
import ru.pixnews.wasm.sqlite.open.helper.graalvm.GraalvmEmbedderBuilder.Companion.SQLITE3_SOURCE_NAME
import ru.pixnews.wasm.sqlite.open.helper.graalvm.GraalvmEmbedderBuilder.GraalvmEmbedderInfo
import ru.pixnews.wasm.sqlite.open.helper.graalvm.exports.GraalvmDynamicMemoryExports
import ru.pixnews.wasm.sqlite.open.helper.graalvm.exports.GraalvmEmscriptenMainExports
import ru.pixnews.wasm.sqlite.open.helper.graalvm.exports.GraalvmEmscriptenPthreadExports
import ru.pixnews.wasm.sqlite.open.helper.graalvm.exports.GraalvmEmscriptenStackExports
import ru.pixnews.wasm.sqlite.open.helper.graalvm.exports.GraalvmIndirectFunctionProvider
import ru.pixnews.wasm.sqlite.open.helper.graalvm.exports.GraalvmSqliteExports
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.GraalvmEmscriptenRuntime
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory.GraalvmWasmHostMemoryAdapter
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory.SharedMemoryWaiterListStore
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.sqlitecb.GraalvmSqliteCallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.GraalvmPthreadManager
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.ManagedThreadInitializer
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.threadfactory.GraalvmManagedThreadFactory
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.memory.DynamicMemory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.EmscriptenPthread
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.EmscriptenPthreadInternal
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructPthread
import java.util.concurrent.ThreadFactory

internal class GraalvmSqliteRuntimeInternal internal constructor(
    private val mainThreadGraalContext: Context,
    internal val envModuleInstance: WasmInstance,
    host: EmbedderHost,
    embedderInfo: GraalvmEmbedderInfo,
    override val callbackStore: SqliteCallbackStore,
    override val callbackFunctionIndexes: GraalvmSqliteCallbackFunctionIndexes,
    internal val memoryWaiters: SharedMemoryWaiterListStore,
    private val embedderBuilder: GraalvmEmbedderBuilder,
) : SqliteRuntimeInternal<GraalvmRuntime>, ManagedThreadInitializer {
    private val logger = host.rootLogger.withTag("GraalvmSqliteWasmEnvironment")
    private val _localGraalContext = ThreadLocal<Context>().also {
        it.set(mainThreadGraalContext)
    }

    private val localGraalContext: Context
        get() {
            val threadContext = _localGraalContext.get()
            if (threadContext != null) {
                return threadContext
            } else {
                logger.e {
                    "Graal Wasm environment is not initialized for thread ${Thread.currentThread().name} " +
                            "and cannot be used to call WASM functions"
                }
                _localGraalContext.set(mainThreadGraalContext)
                return mainThreadGraalContext
            }
        }

    private val mainBindings: Value
        get() = requireNotNull(localGraalContext.getBindings("wasm").getMember(SQLITE3_SOURCE_NAME)) {
            "SQLite module not loaded"
        }

    private val emscriptenMainExports = GraalvmEmscriptenMainExports(::mainBindings)
    private val stackExports = GraalvmEmscriptenStackExports(::mainBindings)
    private val pthreadExports = GraalvmEmscriptenPthreadExports(::mainBindings)
    private val dynamicMemoryExports = GraalvmDynamicMemoryExports(::mainBindings)
    private val indirectFunctionBindingProvider = GraalvmIndirectFunctionProvider(::mainBindings)
    override val sqliteExports: SqliteExports = GraalvmSqliteExports(::mainBindings)
    private val dynamicMemory = DynamicMemory(dynamicMemoryExports)
    override val memory = GraalvmWasmHostMemoryAdapter(envModuleInstance, null)
    private val emscriptenPthread = EmscriptenPthread(pthreadExports, dynamicMemory, memory)
    private val emscriptenPthreadInternal = EmscriptenPthreadInternal(pthreadExports)
    val pthreadManager = GraalvmPthreadManager(
        emscriptenPthreadInternal = emscriptenPthreadInternal,
        emscriptenPthread = emscriptenPthread,
        dynamicMemory = dynamicMemory,
        memory = memory,
        indirectFunctionBindingProvider = indirectFunctionBindingProvider,
        managedThreadInitializer = this,
        useManagedThreadPthreadRoutineFunction = callbackFunctionIndexes.useManagedThreadPthreadRoutineFunction,
        rootLogger = host.rootLogger,
    )
    override val runtimeInstance: GraalvmRuntime = object : GraalvmRuntime {
        override val embedderInfo: SqliteEmbedderRuntimeInfo = embedderInfo
        override val managedThreadFactory: ThreadFactory = GraalvmManagedThreadFactory(
            emscriptenPthread = emscriptenPthread,
            emscriptenPthreadInternal = emscriptenPthreadInternal,
            pthreadManager = pthreadManager,
            managedThreadInitializer = this@GraalvmSqliteRuntimeInternal,
            rootLogger = host.rootLogger,
        )
    }
    val emscriptenRuntime = GraalvmEmscriptenRuntime.multithreadedRuntime(
        mainExports = emscriptenMainExports,
        stackExports = stackExports,
        emscriptenPthread = emscriptenPthread,
        emscriptenPthreadInternal = emscriptenPthreadInternal,
        memory = memory,
        logger = host.rootLogger,
    )

    override fun initThreadLocalGraalvmAgent() {
        val newContext = embedderBuilder.initChildThreadContext(this)
        check(_localGraalContext.get() == null) {
            "Graalvm agent already initialized in this thread"
        }
        _localGraalContext.set(newContext)
    }

    override fun destroyThreadLocalGraalvmAgent() {
        logger.v { "destroyThreadLocalGraalvmAgent()" }
        val context = _localGraalContext.get()
        _localGraalContext.remove()
        check(context != mainThreadGraalContext) {
            "destroyThreadLocalGraalvmAgent() should not be called from the main thread"
        }

        // Does not close the context in the dying thread, as this leads to the closing of shared memory
        // used in other threads
        // context?.close()
    }

    override fun initWorkerThread(
        threadPtr: WasmPtr<StructPthread>,
    ) {
        emscriptenRuntime.initWorkerThread(threadPtr)
    }

    override fun close() {
        logger.v { "close()" }
        val context = _localGraalContext.get()
        check(context == mainThreadGraalContext) {
            "close() should be called from the main thread"
        }
        // Before closing we need to wait for all child threads to complete otherwise they may use already destroyed
        // shared memory
        pthreadManager.joinThreads()

        _localGraalContext.remove()
        mainThreadGraalContext.close()
    }
}
