/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("VariableNaming", "PropertyName")

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.pthread

import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmFunctionBinding

public interface EmscriptenPthreadExports {
    public var __tls_base: Int
    public val __tls_size: Int
    public val __tls_align: Int
    public var g_needs_dynamic_alloc: Int
    public var thread_ptr: Int
    public var is_main_thread: Int
    public var is_runtime_thread: Int
    public var supports_wait: Int
    public val _emscripten_check_mailbox: WasmFunctionBinding
    public val _emscripten_run_on_main_thread_js: WasmFunctionBinding
    public val _emscripten_thread_crashed: WasmFunctionBinding
    public val _emscripten_thread_exit: WasmFunctionBinding
    public val _emscripten_thread_free_data: WasmFunctionBinding
    public val _emscripten_thread_init: WasmFunctionBinding
    public val _emscripten_tls_init: WasmFunctionBinding
    public val emscripten_main_runtime_thread_id: WasmFunctionBinding
    public val emscripten_main_thread_process_queued_calls: WasmFunctionBinding
    public val emscripten_stack_set_limits: WasmFunctionBinding
    public val pthread_self: WasmFunctionBinding
}
