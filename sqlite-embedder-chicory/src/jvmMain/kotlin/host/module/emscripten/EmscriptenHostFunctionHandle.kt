/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value

internal fun interface EmscriptenHostFunctionHandle {
    fun apply(instance: Instance, vararg args: Value): Value?
}
