/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructPthread

public class PthreadManager(
    public val exports: EmscriptenPthreadExports,
    rootLogger: Logger,
    public val isMainThread: () -> Boolean,
) {
    private val logger: Logger = rootLogger.withTag("GraalvmPthreadManager")

    /**
     * Handler for `_emscripten_init_main_thread_js`
     *
     * Called from  `__wasm_call_ctors` -> `_emscripten_init_main_thread` during the initial initialization of the
     * main thread of the webassembly environment
     */
    public fun initMainThreadJs(
        threadPtr: WasmPtr<StructPthread>,
    ) {
        check(isMainThread()) { "Should be called on main thread" }

        emscriptenThreadInit(
            threadPtr = threadPtr,
            isMain = true,
            isRuntime = true,
            canBlock = true,
            defaultStackSize = DEFAULT_THREAD_STACK_SIZE,
            startProfiling = false,
        )
        threadInitTls()
    }

    @Suppress("COMMENTED_OUT_CODE")
    public fun initWorkerThread() {
//        readyPromiseResolve(Module); //
//        runtimeInitialized = true
//        startWorker(Module);
    }

    /**
     * Runs `_emscripten_tls_init` export to initialize thread-local storage
     */
    public fun threadInitTls() {
        logger.v { "threadInitTls" }
        exports._emscripten_tls_init.executeVoid()
    }

//    public fun pthreadSelf(): WasmPtr<> {
//
//    }

    public fun emscriptenThreadInit(
        threadPtr: WasmPtr<StructPthread>,
        isMain: Boolean,
        isRuntime: Boolean = true, //
        canBlock: Boolean = true, // !ENVIRONMENT_IS_WEB
        defaultStackSize: Int = 524288,
        startProfiling: Boolean = false,
    ) {
        logger.v {
            "emscriptenThreadInit($threadPtr, $isMain, $isRuntime, $canBlock, $defaultStackSize, $startProfiling)"
        }
        exports._emscripten_thread_init.executeVoid(
            threadPtr.addr,
            isMain.toInt(),
            isRuntime.toInt(),
            canBlock.toInt(),
            defaultStackSize,
            startProfiling.toInt(),
        )
    }

    internal companion object {
        internal const val DEFAULT_THREAD_STACK_SIZE = 524288

        private fun Boolean.toInt(
            trueValue: Int = 1,
            falseValue: Int = 0,
        ) = if (this) trueValue else falseValue
    }
}
