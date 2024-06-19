/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.ManagedThreadBase.State.DESTROYING
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.threadfactory.ExternalManagedThreadOrchestrator
import ru.pixnews.wasm.sqlite.open.helper.host.base.binding.IndirectFunctionBindingProvider
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.IndirectFunctionTableIndex
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.memory.DynamicMemory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.EmscriptenPthread
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.EmscriptenPthreadInternal
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.PthreadManager
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructPthread
import ru.pixnews.wasm.sqlite.open.helper.host.include.pthread_t
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class GraalvmPthreadManager(
    memory: Memory,
    dynamicMemory: DynamicMemory,
    private val useManagedThreadPthreadRoutineFunction: IndirectFunctionTableIndex,
    private val managedThreadInitializer: ManagedThreadInitializer,
    private val indirectFunctionBindingProvider: IndirectFunctionBindingProvider,
    private val emscriptenPthreadInternal: EmscriptenPthreadInternal,
    private val emscriptenPthread: EmscriptenPthread,
    private val mainThreadId: Long = Thread.currentThread().id,
    rootLogger: Logger,
) : PthreadManager(
    emscriptenPthreadInternal,
    { mainThreadId == Thread.currentThread().id },
) {
    private val logger: Logger = rootLogger.withTag("GraalvmPthreadManager")
    private val lock = ReentrantLock()
    private val threads = NativeThreadRegistry()
    private val threadNumber = AtomicInteger(1)
    private val externalThreadOrchestrator = ExternalManagedThreadOrchestrator(
        pthread = emscriptenPthread,
        dynamicMemory = dynamicMemory,
        memory = memory,
        useManagedThreadPthreadRoutineFunction = useManagedThreadPthreadRoutineFunction,
        rootLogger = rootLogger,
    )

    public fun createWasmPthreadForThread(thread: Thread): pthread_t =
        externalThreadOrchestrator.createWasmPthreadForThread(thread)

    /**
     * Called from `__pthread_create_js` to reuse an existing Web Worker or spawn a new one
     */
    fun spawnThread(
        pthreadPtr: WasmPtr<StructPthread>,
        attr: WasmPtr<UInt>,
        startRoutine: Int,
        arg: WasmPtr<Unit>,
    ): Int {
        logger.v { "spawnThread($pthreadPtr, $attr, $startRoutine, $arg)" }
        return if (startRoutine != useManagedThreadPthreadRoutineFunction.funcId) {
            spawnManagedThread(pthreadPtr, startRoutine, arg)
        } else {
            lock.withLock {
                val thread = externalThreadOrchestrator.joinExternalThread(arg)
                threads.register(pthreadPtr, thread)
                0
            }
        }
    }

    private fun spawnManagedThread(
        pthreadPtr: WasmPtr<StructPthread>,
        startRoutine: Int,
        arg: WasmPtr<Unit>,
    ): Int {
        val name = "graalvm-pthread-${threadNumber.getAndDecrement()}"
        val thread = ManagedPthread(
            name = name,
            pthreadPtr = pthreadPtr,
            startRoutine = startRoutine,
            arg = arg,
            threadInitializer = managedThreadInitializer,
            indirectBindingProvider = indirectFunctionBindingProvider,
            emscriptenPthread = emscriptenPthread,
            emscriptenPthreadInternal = emscriptenPthreadInternal,
            stateListener = { thread, ptr, newState ->
                logger.v { "Thread $ptr: $newState" }

                when (newState) {
                    DESTROYING -> unregisterManagedThread(ptr!!, thread)
                    else -> {}
                }
            },
        )
        thread.setUncaughtExceptionHandler { terminatedThread, throwable ->
            val ptr = (terminatedThread as ManagedPthread).pthreadPtr
            logger.i(throwable) { "Uncaught exception in Pthread $ptr ${terminatedThread.name} " }
            if (ptr != null) {
                unregisterManagedThread(ptr, thread, throwable)
            }
            throw throwable
        }

        lock.withLock {
            threads.register(pthreadPtr, thread)
        }
        thread.start()

        return 0
    }

    fun unregisterManagedThread(
        pthreadPtr: WasmPtr<StructPthread>,
        thread: Thread,
        throwable: Throwable? = null,
    ) = lock.withLock {
        threads.unregister(pthreadPtr, thread, throwable)
    }

    private class NativeThreadRegistry {
        private val threads: MutableMap<WasmPtr<StructPthread>, Thread> = mutableMapOf()

        fun register(
            ptr: WasmPtr<StructPthread>,
            thread: Thread,
        ) {
            val old = threads.getOrPut(ptr) { thread }
            check(old == thread) {
                "Another thread already registered for $ptr"
            }
        }

        fun unregister(
            ptr: WasmPtr<StructPthread>,
            terminatedThread: Thread,
            error: Throwable? = null,
        ) {
            val oldThread = threads.remove(ptr)
            @Suppress("UseCheckOrError")
            if (oldThread != terminatedThread) {
                throw IllegalStateException("Removed wrong thread $oldThread").also {
                    if (error != null) {
                        it.addSuppressed(error)
                    }
                }
            }
        }
    }
}
