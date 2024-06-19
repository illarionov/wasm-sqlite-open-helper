/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.plus
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.EmscriptenMainExports
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.EmscriptenRuntime
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.EmscriptenPthread
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.EmscriptenPthreadInternal
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.stack.EmscriptenStackExports
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructPthread
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructPthread.Companion.STRUCT_PTHREAD_STACK_HIGH_OFFSET
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructPthread.Companion.STRUCT_PTHREAD_STACK_SZIE_OFFSET

internal class GraalvmEmscriptenRuntime(
    mainExports: EmscriptenMainExports,
    stackExports: EmscriptenStackExports,
    memory: Memory,
    private val emscriptenPthread: EmscriptenPthread?,
    private val emscriptenPthreadInternal: EmscriptenPthreadInternal?,
    rootLogger: Logger,
) : EmscriptenRuntime(mainExports, stackExports, memory, rootLogger) {
    public override val isMultiThread: Boolean get() = emscriptenPthreadInternal != null

    public override fun initMainThread(): Unit = if (isMultiThread) {
        initMultithreadedMainThread()
    } else {
        initSingleThreadedMainThread()
    }

    private fun initMultithreadedMainThread() {
        stack.stackCheckInit(memory)
        stack.setStackLimits()
        mainExports.__wasm_call_ctors.executeVoid()
        stack.checkStackCookie(memory)
    }

    public fun initWorkerThread(
        threadPtr: WasmPtr<StructPthread>,
    ) {
        val pthreadInternal = checkNotNullInMultithreaded(emscriptenPthreadInternal)

        pthreadInternal.emscriptenThreadInit(
            threadPtr,
            isMain = false,
            isRuntime = false,
            canBlock = true,
            defaultStackSize = 0,
            startProfiling = false,
        )
        establishStackSpace()
        pthreadInternal.emscriptenTlsInit()
    }

    private fun establishStackSpace() {
        val pthread = checkNotNullInMultithreaded(emscriptenPthread)

        val pthreadPtr: WasmPtr<StructPthread> = WasmPtr(pthread.pthreadSelf().toInt())
        val stackHigh: Int = memory.readI32(pthreadPtr + STRUCT_PTHREAD_STACK_HIGH_OFFSET)
        val stackSize: Int = memory.readI32(pthreadPtr + STRUCT_PTHREAD_STACK_SZIE_OFFSET)

        val stackLow = stackHigh - stackSize
        check(stackHigh != 0 && stackLow != 0)
        check(stackHigh > stackLow) { "stackHigh must be higher then stackLow" }
        check(stackLow != 0)

        stack.emscriptenStackSetLimits(WasmPtr(stackHigh), WasmPtr(stackLow))
        stack.setStackLimits()
        stack.emscriptenStackRestore(WasmPtr(stackHigh))
        stack.writeStackCookie(memory)
    }

    private fun <T : Any> checkNotNullInMultithreaded(value: T?): T = checkNotNull(value) {
        "This function should not be called in a single threaded environment"
    }

    public companion object {
        public fun multithreadedRuntime(
            mainExports: EmscriptenMainExports,
            stackExports: EmscriptenStackExports,
            memory: Memory,
            emscriptenPthread: EmscriptenPthread,
            emscriptenPthreadInternal: EmscriptenPthreadInternal,
            logger: Logger,
        ): GraalvmEmscriptenRuntime {
            return GraalvmEmscriptenRuntime(
                mainExports = mainExports,
                stackExports = stackExports,
                emscriptenPthread = emscriptenPthread,
                emscriptenPthreadInternal = emscriptenPthreadInternal,
                memory = memory,
                rootLogger = logger,
            )
        }
    }
}
