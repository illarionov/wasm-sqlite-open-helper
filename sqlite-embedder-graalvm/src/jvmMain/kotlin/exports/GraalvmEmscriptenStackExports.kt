/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.exports

import org.graalvm.polyglot.Value
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.functionMember
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.intGlobalMember
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.optionalFunctionMember
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.optionalIntGlobalMember
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmFunctionBinding
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.stack.EmscriptenStackExports

internal class GraalvmEmscriptenStackExports(
    mainBindings: Value,
) : EmscriptenStackExports {
    override var __stack_pointer: Int by mainBindings.intGlobalMember()
    override var __stack_end: Int? by mainBindings.optionalIntGlobalMember()
    override var __stack_base: Int? by mainBindings.optionalIntGlobalMember()
    override val __set_stack_limits: WasmFunctionBinding? by mainBindings.optionalFunctionMember()
    override val emscripten_stack_init by mainBindings.optionalFunctionMember()
    override val emscripten_stack_get_free by mainBindings.optionalFunctionMember()
    override val emscripten_stack_get_base by mainBindings.optionalFunctionMember()
    override val emscripten_stack_get_end by mainBindings.optionalFunctionMember()
    override val emscripten_stack_get_current by mainBindings.functionMember()
    override val emscripten_stack_set_limits by mainBindings.functionMember()
    override val _emscripten_stack_alloc by mainBindings.functionMember()
    override val _emscripten_stack_restore by mainBindings.functionMember()
}
