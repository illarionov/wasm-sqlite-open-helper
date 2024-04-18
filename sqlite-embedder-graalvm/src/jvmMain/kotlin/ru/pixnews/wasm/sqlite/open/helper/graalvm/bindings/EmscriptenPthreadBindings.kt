/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("VariableNaming", "VARIABLE_HAS_PREFIX", "BLANK_LINE_BETWEEN_PROPERTIES")

package ru.pixnews.wasm.sqlite.open.helper.graalvm.bindings

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.member
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.toInt

internal class EmscriptenPthreadBindings(
    val context: Context,
    mainBindings: Value,
) {
    val gNeedsDynamicAlloc = mainBindings.getMember("\$g_needs_dynamic_alloc")
    val isMainThreadGlobal = mainBindings.getMember("\$is_main_thread")
    val isRuntimeThreadGlobal = mainBindings.getMember("\$is_runtime_thread")
    val supportsWaitGlobal = mainBindings.getMember("\$supports_wait")
    val threadPtrGlobal = mainBindings.getMember("\$thread_ptr")
    val tlsBaseGlobal = mainBindings.getMember("\$__tls_base")

    val _emscripten_check_mailbox by mainBindings.member()
    val _emscripten_run_on_main_thread_js by mainBindings.member()
    val _emscripten_thread_crashed by mainBindings.member()
    val _emscripten_thread_exit by mainBindings.member()
    val _emscripten_thread_free_data by mainBindings.member()
    private val _emscripten_thread_init by mainBindings.member()
    private val _emscripten_tls_init by mainBindings.member()
    val emscripten_main_runtime_thread_id by mainBindings.member()
    val emscripten_main_thread_process_queued_calls by mainBindings.member()
    val emscripten_stack_set_limits by mainBindings.member()
    val pthread_self by mainBindings.member()

    fun emscriptenTlsInit() {
        _emscripten_tls_init.executeVoid()
    }

    fun emscriptenThreadInit(
        threadPtr: WasmPtr<*>,
        isMain: Boolean, // !ENVIRONMENT_IS_WORKER
        isRuntime: Boolean = true, //
        canBlock: Boolean = true, // !ENVIRONMENT_IS_WEB
        defaultStackSize: Int = 524288,
        startProfiling: Boolean = false,
    ) {
        _emscripten_thread_init.executeVoid(
            threadPtr.addr,
            isMain.toInt(),
            isRuntime.toInt(),
            canBlock.toInt(),
            defaultStackSize,
            startProfiling.toInt(),
        )
    }
}
