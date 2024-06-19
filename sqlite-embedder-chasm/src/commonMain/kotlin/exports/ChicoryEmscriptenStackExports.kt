/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.exports

import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.functionMember
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.intGlobalMember
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.optionalFunctionMember
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.optionalIntGlobalMember
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.ChasmInstanceBuilder.ChasmInstance
import ru.pixnews.wasm.sqlite.open.helper.host.base.binding.WasmFunctionBinding
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.stack.EmscriptenStackExports

internal class ChicoryEmscriptenStackExports(instance: ChasmInstance) : EmscriptenStackExports {
    override var __stack_pointer: Int by instance.intGlobalMember()
    override var __stack_end: Int? by instance.optionalIntGlobalMember()
    override var __stack_base: Int? by instance.optionalIntGlobalMember()
    override val __set_stack_limits: WasmFunctionBinding? by instance.optionalFunctionMember()
    override val emscripten_stack_init: WasmFunctionBinding? by instance.optionalFunctionMember()
    override val emscripten_stack_get_free: WasmFunctionBinding? by instance.optionalFunctionMember()
    override val emscripten_stack_get_base: WasmFunctionBinding? by instance.optionalFunctionMember()
    override val emscripten_stack_get_end: WasmFunctionBinding? by instance.optionalFunctionMember()
    override val emscripten_stack_get_current: WasmFunctionBinding by instance.functionMember()
    override val emscripten_stack_set_limits: WasmFunctionBinding by instance.functionMember()
    override val _emscripten_stack_alloc: WasmFunctionBinding by instance.functionMember()
    override val _emscripten_stack_restore: WasmFunctionBinding by instance.functionMember()
}
