/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("PropertyName", "VariableNaming")

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export

import ru.pixnews.wasm.sqlite.open.helper.host.base.binding.WasmFunctionBinding

/**
 * Interface for calling Emscripten exported functions
 */
public interface EmscriptenMainExports {
    public val _initialize: WasmFunctionBinding?
    public val __errno_location: WasmFunctionBinding
    public val __wasm_call_ctors: WasmFunctionBinding
}