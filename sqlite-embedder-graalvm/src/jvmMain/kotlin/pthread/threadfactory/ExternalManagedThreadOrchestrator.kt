/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.threadfactory

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readU64
import ru.pixnews.wasm.sqlite.open.helper.host.base.POINTER
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.IndirectFunctionTableIndex
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.memory.DynamicMemory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.memory.freeSilent
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.EmscriptenPthread
import ru.pixnews.wasm.sqlite.open.helper.host.include.pthread_t
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class ExternalManagedThreadOrchestrator(
    private val pthread: EmscriptenPthread,
    private val dynamicMemory: DynamicMemory,
    private val memory: Memory,
    private val useManagedThreadPthreadRoutineFunction: IndirectFunctionTableIndex,
    rootLogger: Logger,
) {
    private val logger: Logger = rootLogger.withTag("ExternalThreadOrchestrator")
    private val startingManagedThreads = StartingExternalManagedThreadRegistry(logger)
    private val lock = ReentrantLock()

    public fun createWasmPthreadForThread(thread: Thread): pthread_t {
        val token = registerExternalThread(thread)
        try {
            // XXX: native pthread leaks if thread not started
            val threadId = pthreadCreate(token)
            logger.v { "Pthread $threadId created" }
            check(threadId != 0UL) {
                "thread id is not initialized"
            }
            return threadId
        } catch (@Suppress("TooGenericExceptionCaught") ex: Throwable) {
            unregisterExternalThread(token, thread)
            throw ex
        }
    }

    private fun pthreadCreate(
        token: ManagedThreadToken,
    ): pthread_t {
        var tokenRef: WasmPtr<Long> = WasmPtr.sqlite3Null()
        try {
            tokenRef = dynamicMemory.allocOrThrow(8U)
            memory.writeI64(tokenRef, token.id.toLong())
            return pthread.pthreadCreate(
                attr = WasmPtr.SQLITE3_NULL,
                startRoutine = useManagedThreadPthreadRoutineFunction,
                arg = tokenRef,
            )
        } finally {
            dynamicMemory.freeSilent(tokenRef)
        }
    }

    fun joinExternalThread(
        startRoutineArg: WasmPtr<Unit>,
    ): Thread {
        val threadToken = ManagedThreadToken(memory.readU64(startRoutineArg))
        lock.withLock {
            val thread = startingManagedThreads.unregister(threadToken)
            requireNotNull(thread) {
                "External thread with token `$threadToken` is not registered"
            }
            return thread
        }
    }

    private fun registerExternalThread(
        thread: Thread,
    ): ManagedThreadToken = lock.withLock {
        startingManagedThreads.register(thread)
    }

    private fun unregisterExternalThread(
        token: ManagedThreadToken,
        thread: Thread,
    ) = lock.withLock {
        startingManagedThreads.unregister(token, thread)
    }

    private class StartingExternalManagedThreadRegistry(
        rootLogger: Logger,
    ) {
        private val logger = rootLogger.withTag("StartingExternalManagedThreadRegistry")
        private val threads: MutableMap<ManagedThreadToken, WeakReference<Thread>> = mutableMapOf()
        private val threadToken = AtomicLong()

        fun register(
            thread: Thread,
        ): ManagedThreadToken {
            val token = ManagedThreadToken(threadToken.incrementAndGet().toULong())
            val old = threads.getOrPut(token) { WeakReference(thread) }.get()
            check(old == thread) {
                "Another thread already registered for token $token"
            }
            return token
        }

        fun unregister(
            token: ManagedThreadToken,
            thread: Thread,
        ) {
            val threadReference = threads.remove(token)
            if (threadReference != null) {
                val oldThread = threadReference.get()
                if (oldThread != null) {
                    check(oldThread == thread) {
                        "Registered another thread on token $thread"
                    }
                } else {
                    logger.e { "Thread is garbage-collected. Most likely native StructPthread leaked." }
                }
            }
        }

        fun unregister(
            token: ManagedThreadToken,
        ): Thread? {
            val threadReference = threads.remove(token) ?: return null
            val thread = threadReference.get()
            if (thread == null) {
                logger.e { "Thread is garbage-collected. Most likely native StructPthread leaked." }
            }
            return thread
        }
    }

    @JvmInline
    internal value class ManagedThreadToken(
        val id: ULong,
    )

    companion object {
        public val USE_MANAGED_THREAD_PTHREAD_ROUTINE_FUNCTION = object : HostFunction {
            override val wasmName: String = "use_managed_thread_pthread_routine"
            override val type: HostFunction.HostFunctionType = HostFunction.HostFunctionType(
                params = listOf(POINTER),
                returnTypes = listOf(POINTER),
            )
        }
    }
}
