/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread

import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.ManagedThreadBase.State.ATTACHING
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.ManagedThreadBase.State.DESTROYED
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.ManagedThreadBase.State.DESTROYING
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.ManagedThreadBase.State.DETACHING
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.ManagedThreadBase.State.LOADING
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.ManagedThreadBase.State.RUNNING
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.ManagedThreadBase.StateListener
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.EmscriptenPthread
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.EmscriptenPthreadInternal
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructPthread

internal abstract class ManagedThreadBase(
    name: String,
    private val emscriptenPthread: EmscriptenPthread,
    private val pthreadInternal: EmscriptenPthreadInternal,
    private val threadInitializer: ManagedThreadInitializer,
    private val stateListener: StateListener = StateListener { _, _, _ -> },
) : Thread(name) {
    abstract var pthreadPtr: WasmPtr<StructPthread>?
    private val stateLock = Any()
    private var wasmAgentLoaded: Boolean = false
    private var pthreadAttached: Boolean = false

    @Volatile
    public var state: State = State.NOT_STARTED
        private set(newState) {
            synchronized(stateLock) {
                field = newState
            }
            stateListener.onNewState(this, pthreadPtr, newState)
        }

    override fun run() {
        checkNotNull(pthreadPtr) {
            "pthreadPtr is not set"
        }

        state = LOADING
        try {
            loadWasmAgent()

            state = ATTACHING
            attachPthread()

            state = RUNNING
            managedRun()
        } finally {
            state = DETACHING
            try {
                detachPthread()
            } finally {
                state = DESTROYING
                destroyThreadPtr()
                unloadWasmAgent()

                state = DESTROYED
            }
        }
    }

    abstract fun managedRun()

    private fun loadWasmAgent() {
        threadInitializer.initThreadLocalGraalvmAgent()
        wasmAgentLoaded = true
    }

    private fun unloadWasmAgent() {
        threadInitializer.destroyThreadLocalGraalvmAgent()
        wasmAgentLoaded = false
    }

    private fun attachPthread() {
        val ptr = requireNotNull(pthreadPtr)
        threadInitializer.initWorkerThread(ptr)

        check(emscriptenPthread.pthreadSelf() == ptr.addr.toULong()) {
            "pthreadSelf is not $ptr"
        }

        pthreadAttached = true
    }

    private fun detachPthread() {
        if (!pthreadAttached) {
            return
        }
        pthreadAttached = false
        pthreadInternal.emscriptenThreadExit(WasmPtr<Unit>(-1))
    }

    private fun destroyThreadPtr() {
        val ptr = requireNotNull(pthreadPtr)
        pthreadInternal.emscriptenThreadFreeData(ptr)
        pthreadPtr = null
    }

    public enum class State {
        NOT_STARTED,
        LOADING,
        ATTACHING,
        RUNNING,
        DETACHING,
        DESTROYING,
        DESTROYED,
    }

    internal fun interface StateListener {
        fun onNewState(
            thread: ManagedThreadBase,
            pthreadPtr: WasmPtr<StructPthread>?,
            newState: State,
        )
    }
}
