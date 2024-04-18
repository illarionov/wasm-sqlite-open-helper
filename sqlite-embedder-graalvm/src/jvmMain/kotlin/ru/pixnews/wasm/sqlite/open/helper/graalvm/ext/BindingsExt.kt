/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.ext

import org.graalvm.polyglot.Value
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.WasmFunctionBinding
import ru.pixnews.wasm.sqlite.open.helper.graalvm.bindings.GraalWasmFunctionBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal fun Value.member(): ReadOnlyProperty<Any?, WasmFunctionBinding> = ReadOnlyProperty { _, prop: KProperty<*> ->
    GraalWasmFunctionBinding(this@member.getMember(prop.name) ?: error("No member ${prop.name}"))
}

internal fun Boolean.toInt(
    trueValue: Int = 1,
    falseValue: Int = 0,
) = if (this) trueValue else falseValue
