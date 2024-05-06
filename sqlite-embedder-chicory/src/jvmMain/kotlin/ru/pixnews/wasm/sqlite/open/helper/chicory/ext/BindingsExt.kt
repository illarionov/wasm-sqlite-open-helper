/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.ext

import com.dylibso.chicory.runtime.Instance
import ru.pixnews.wasm.sqlite.open.helper.chicory.bindings.ChicoryWasmFunctionBinding
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmFunctionBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal fun Instance.member(): ReadOnlyProperty<Any?, WasmFunctionBinding> {
    return ReadOnlyProperty { _, prop: KProperty<*> ->
        ChicoryWasmFunctionBinding(this@member.export(prop.name) ?: error("No member ${prop.name}"))
    }
}

internal fun Boolean.toInt(
    trueValue: Int = 1,
    falseValue: Int = 0,
) = if (this) trueValue else falseValue
