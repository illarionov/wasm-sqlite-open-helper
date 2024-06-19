/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.threadfactory

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.GraalvmPthreadManager
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.ManagedThreadBase
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.ManagedThreadBase.State.DESTROYING
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.ManagedThreadInitializer
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.EmscriptenPthread
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.EmscriptenPthreadInternal
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructPthread
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

internal class GraalvmManagedThreadFactory(
    private val emscriptenPthread: EmscriptenPthread,
    private val emscriptenPthreadInternal: EmscriptenPthreadInternal,
    private val pthreadManager: GraalvmPthreadManager,
    private val managedThreadInitializer: ManagedThreadInitializer,
    rootLogger: Logger,
) : ThreadFactory {
    private val logger: Logger = rootLogger.withTag("GraalvmManagedThreadFactory")
    private val threadNumber = AtomicInteger(1)

    override fun newThread(runnable: Runnable): Thread {
        val name = "graalvm-embedder-thread-${threadNumber.getAndDecrement()}"
        val threadStateListener = ManagedThreadBase.StateListener { thread, ptr, newState ->
            logger.v { "Thread $ptr: $newState" }
            when (newState) {
                DESTROYING -> if (ptr != null) {
                    pthreadManager.unregisterManagedThread(ptr, thread)
                }
                else -> {}
            }
        }

        val thread = object : ManagedThreadBase(
            name = name,
            emscriptenPthread = emscriptenPthread,
            pthreadInternal = emscriptenPthreadInternal,
            threadInitializer = managedThreadInitializer,
            stateListener = threadStateListener,
        ) {
            override var pthreadPtr: WasmPtr<StructPthread>? = null

            override fun managedRun() = runnable.run()
        }
        if (thread.isDaemon) {
            thread.isDaemon = false
        }

        // XXX: Wasm pthread leaks if thread not started
        val ptr = pthreadManager.createWasmPthreadForThread(thread)
        thread.pthreadPtr = WasmPtr(ptr.toInt())

        return thread
    }
}
