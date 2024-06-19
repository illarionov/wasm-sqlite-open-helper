/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.exports

import com.dylibso.chicory.runtime.Instance
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.functionMember
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.optionalFunctionMember
import ru.pixnews.wasm.sqlite.open.helper.host.base.binding.WasmFunctionBinding
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.EmscriptenMainExports

internal class ChicoryEmscriptenMainExports(instance: Instance) : EmscriptenMainExports {
    override val _initialize: WasmFunctionBinding? by instance.optionalFunctionMember()
    override val __errno_location: WasmFunctionBinding by instance.functionMember()
    override val __wasm_call_ctors: WasmFunctionBinding by instance.functionMember()
}
