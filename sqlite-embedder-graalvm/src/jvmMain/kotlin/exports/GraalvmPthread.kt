/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("VariableNaming", "BLANK_LINE_BETWEEN_PROPERTIES")

package ru.pixnews.wasm.sqlite.open.helper.graalvm.bindings

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.toInt
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread.EmscriptenPthreadExports

internal class GraalvmPthread(
    private val exports: EmscriptenPthreadExports,
    rootLogger: Logger,
    @Suppress("DEPRECATION")
    private val mainThreadId: Long = Thread.currentThread().id,
) {
    private val logger: Logger = rootLogger.withTag("Pthread")

    init {
        // TODO: worker
        // PThread["setExitStatus"] = PThread.setExitStatus;
        // noExitRuntime = false;
        // EXITSTATUS
    }

    public fun emscriptenThreadLocalStorageInit() {
        logger.v { "emscriptenThreadLocalStorageInit()" }
        exports._emscripten_tls_init.executeVoid()
    }

    public fun emscriptenThreadInit(
        threadPtr: WasmPtr<*>,
        isMain: Boolean, // !ENVIRONMENT_IS_WORKER
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

    public fun isMainThread(): Boolean {
        @Suppress("DEPRECATION")
        return Thread.currentThread().id == mainThreadId
    }

    internal companion object {
        internal const val DEFAULT_THREAD_STACK_SIZE = 524288
    }
}
