/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readU64
import ru.pixnews.wasm.sqlite.open.helper.embedder.exports.sqliteFreeSilent
import ru.pixnews.wasm.sqlite.open.helper.graalvm.exports.GraalSqliteMemoryExports
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.sqlitecb.GraalvmSqliteCallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.WasmManagedThreadStore.ManagedThread
import ru.pixnews.wasm.sqlite.open.helper.host.base.POINTER
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunction.HostFunctionType
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.PthreadManager
import ru.pixnews.wasm.sqlite.open.helper.host.include.pthread_t
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

internal class GraalvmManagedThreadFactory(
    private val threadManager: PthreadManager,
    private val managedThreadStore: WasmManagedThreadStore,
    private val dynamicMemory: GraalSqliteMemoryExports,
    private val memory: Memory,
    private val indirectFunctionIndexes: GraalvmSqliteCallbackFunctionIndexes,
    rootLogger: Logger,
) : ThreadFactory {
    private val logger: Logger = rootLogger.withTag("GraalvmManagedThreadFactory")
    private val threadNumber = AtomicInteger(1)

    override fun newThread(runnable: Runnable): Thread {

        val thread = startSpawningWasmManagedThread(runnable)

        return thread.thread
    }

    private fun startSpawningWasmManagedThread(
        runnable: Runnable
    ) : ManagedThread {
        check(threadManager.isMainThread())

        val threadIdRef = dynamicMemory.sqliteAllocOrThrow<pthread_t>(8U)
        try {
            val threadId = memory.readU64(threadIdRef)
            val startRoutine = indirectFunctionIndexes.pthreadCreateCallbackFunction.funcId
            val pthreadAttrT = WasmPtr.SQLITE3_NULL
            val arg = WasmPtr.SQLITE3_NULL // TODO

            val errNo = requireNotNull(threadManager.exports.pthread_create) {
                "pthread_create not exported." +
                        " Recompile application with _pthread_create and _pthread_exit in EXPORTED_FUNCTIONS"
            }.executeForInt(threadIdRef, pthreadAttrT, startRoutine, arg)
            if (errNo != 0) {
                throw SpawnThreadException("pthread_create() failed with error $errNo", errNo)
            }

            // TODO

            return ManagedThread(
                pthreadId = threadId,
                thread = spawnJvmThread(runnable)
            )
        } finally {
            dynamicMemory.sqliteFreeSilent(threadIdRef)
        }
    }

    fun spawnJvmThread(
        runnable: Runnable
    ): Thread {
        val name = "graalvm-embedder-thread-${threadNumber.getAndDecrement()}"
        val thread = Thread(null, runnable, name)
        if (thread.isDaemon) {
            thread.isDaemon = false
        }
        return thread
    }

    internal class

    public companion object {
        public val PTHREAD_ROUTINE_CALLBACK_FUNCTION = object : HostFunction {
            override val wasmName: String = "pthread_routine_callback"
            override val type: HostFunctionType = HostFunctionType(
                params = listOf(POINTER),
                returnTypes = listOf(POINTER)
            )
        }
    }
}
