/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.exports

import org.graalvm.polyglot.Value
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.optionalFunctionMember
import ru.pixnews.wasm.sqlite.open.helper.host.base.binding.WasmFunctionBinding
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.memory.DynamicMemoryExports

internal class GraalvmDynamicMemoryExports(
    mainBindings: () -> Value,
) : DynamicMemoryExports {
    override val malloc: WasmFunctionBinding? by mainBindings.optionalFunctionMember()
    override val free: WasmFunctionBinding? by mainBindings.optionalFunctionMember()
}
