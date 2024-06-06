/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("PropertyName", "VariableNaming")

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.stack

import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmFunctionBinding

/**
 * Interface for calling exported functions related to the Emscripten stack
 */
public interface EmscriptenStackExports {
    public var __stack_pointer: Int
    public var __stack_end: Int?
    public var __stack_base: Int?
    public val emscripten_stack_init: WasmFunctionBinding?
    public val emscripten_stack_get_free: WasmFunctionBinding?
    public val emscripten_stack_get_base: WasmFunctionBinding?
    public val emscripten_stack_get_end: WasmFunctionBinding?
    public val emscripten_stack_get_current: WasmFunctionBinding
    public val emscripten_stack_set_limits: WasmFunctionBinding
    public val _emscripten_stack_alloc: WasmFunctionBinding
    public val _emscripten_stack_restore: WasmFunctionBinding
}
