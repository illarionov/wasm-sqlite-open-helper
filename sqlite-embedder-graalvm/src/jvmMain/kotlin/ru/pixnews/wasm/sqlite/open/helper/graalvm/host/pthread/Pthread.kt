/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.pthread

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.bindings.EmscriptenPthreadBindings

internal class Pthread(
    rootLogger: Logger,
    @Suppress("DEPRECATION")
    private val mainThreadId: Long = Thread.currentThread().id,
    private val emscriptenPthreadBindings: EmscriptenPthreadBindings,
) {
    private val logger: Logger = rootLogger.withTag(Pthread::class.qualifiedName!!)

    init {
        // TODO: worker
        // PThread["setExitStatus"] = PThread.setExitStatus;
        // noExitRuntime = false;
        // EXITSTATUS
    }

    fun emscriptenThreadInit(
        threadPtr: WasmPtr<*>,
        isMain: Boolean,
        isRuntime: Boolean,
        canBlock: Boolean,
        defaultStackSize: Int,
        startProfiling: Boolean,
    ) {
        logger.v {
            "emscriptenThreadInit($threadPtr, $isMain, $isRuntime, $canBlock, $defaultStackSize, $startProfiling)"
        }
        emscriptenPthreadBindings.emscriptenThreadInit(
            threadPtr,
            isMain,
            isRuntime,
            canBlock,
            defaultStackSize,
            startProfiling,
        )
    }

    public fun emscriptenThreadLocalStorageInit() {
        logger.v { "emscriptenThreadLocalStorageInit()" }
        emscriptenPthreadBindings.emscriptenTlsInit()
    }

    public fun isMainThread(): Boolean {
        @Suppress("DEPRECATION")
        return Thread.currentThread().id == mainThreadId
    }
}
