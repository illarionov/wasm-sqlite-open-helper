/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.EmscriptenPthreadExports
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.PthreadManager
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.stack.EmscriptenStack
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.stack.EmscriptenStackExports
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructPthread

public class EmscriptenRuntime private constructor(
    public val mainExports: EmscriptenMainExports,
    public val stackExports: EmscriptenStackExports,
    public val pthreadExports: EmscriptenPthreadExports?,
    private val isMainThread: () -> Boolean,
    rootLogger: Logger,
) {
    private val logger: Logger = rootLogger.withTag("EmscriptenRuntime")
    public val stack: EmscriptenStack = EmscriptenStack(stackExports, logger)

    @Suppress("NULLABLE_PROPERTY_TYPE")
    public val pthreadManager: PthreadManager? = pthreadExports?.let {
        PthreadManager(it, rootLogger, isMainThread)
    }
    public val isMultiThread: Boolean get() = pthreadExports != null

    public fun initRuntimeMainThread(memory: Memory): Unit = if (isMultiThread) {
        initMultithreadRuntimeMainThread(memory)
    } else {
        initSingleThreadedRuntime(memory)
    }

    private fun initSingleThreadedRuntime(memory: Memory) {
        stack.stackCheckInit(memory)
        mainExports.__wasm_call_ctors.executeVoid()

        stack.checkStackCookie(memory)
    }

    private fun initMultithreadRuntimeMainThread(memory: Memory) {
        stack.stackCheckInit(memory)
        stack.setStackLimits()
        mainExports.__wasm_call_ctors.executeVoid()
        stack.checkStackCookie(memory)
    }

    public fun runWorkerThread(
        threadPtr: WasmPtr<StructPthread>,
    ) {
        val pthread = pthreadManager
        check(pthread != null) { "Should not be called in single-threaded environment" }

        pthread.emscriptenThreadInit(
            threadPtr,
            isMain = false,
            isRuntime = false,
            canBlock = true,
            defaultStackSize = 0,
            startProfiling = false,
        )
//        __emscripten_thread_mailbox_await(msgData["pthread_ptr"]);
        establishStackSpace()
//        PThread.receiveObjectTransfer(msgData);
        pthread.threadInitTls()

//        invokeEntryPoint(msgData["start_routine"], msgData["arg"]);
    }

    private fun establishStackSpace() {
//        var pthread_ptr = _pthread_self();
//        var stackHigh = GROWABLE_HEAP_U32()[(((pthread_ptr) + (52)) >> 2)];
//        var stackSize = GROWABLE_HEAP_U32()[(((pthread_ptr) + (56)) >> 2)];
//        var stackLow = stackHigh - stackSize;
//        assert(stackHigh != 0);
//        assert(stackLow != 0);
//        assert(stackHigh > stackLow, "stackHigh must be higher then stackLow");
//        _emscripten_stack_set_limits(stackHigh, stackLow);
//        setStackLimits();
//        stackRestore(stackHigh);
//        writeStackCookie();
    }

    public companion object {
        public fun emscriptenSingleThreadedRuntime(
            mainExports: EmscriptenMainExports,
            stackExports: EmscriptenStackExports,
            logger: Logger,
        ): EmscriptenRuntime = EmscriptenRuntime(
            mainExports = mainExports,
            stackExports = stackExports,
            pthreadExports = null,
            isMainThread = { true },
            logger,
        )

        @Suppress("LAMBDA_IS_NOT_LAST_PARAMETER")
        public fun emscriptenMultithreadedRuntime(
            mainExports: EmscriptenMainExports,
            stackExports: EmscriptenStackExports,
            pthreadExports: EmscriptenPthreadExports,
            isMainThread: () -> Boolean,
            logger: Logger,
        ): EmscriptenRuntime {
            return EmscriptenRuntime(
                mainExports = mainExports,
                stackExports = stackExports,
                pthreadExports = pthreadExports,
                isMainThread = isMainThread,
                rootLogger = logger,
            )
        }
    }
}
