/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.ext

import org.graalvm.polyglot.Value
import ru.pixnews.wasm.sqlite.open.helper.graalvm.bindings.GraalWasmFunctionBinding
import ru.pixnews.wasm.sqlite.open.helper.host.base.BINDING_NOT_INITIALIZED
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmFunctionBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal fun Value.member(): ReadOnlyProperty<Any?, WasmFunctionBinding> {
    return object : ReadOnlyProperty<Any?, WasmFunctionBinding> {
        @Volatile
        private var binding: WasmFunctionBinding = BINDING_NOT_INITIALIZED

        override fun getValue(thisRef: Any?, property: KProperty<*>): WasmFunctionBinding {
            return binding.let { cachedInstance ->
                if (cachedInstance != BINDING_NOT_INITIALIZED) {
                    cachedInstance
                } else {
                    val export = this@member.getMember(property.name) ?: error("No member ${property.name}")
                    GraalWasmFunctionBinding(export).also {
                        this.binding = it
                    }
                }
            }
        }
    }
}

internal fun Boolean.toInt(
    trueValue: Int = 1,
    falseValue: Int = 0,
) = if (this) trueValue else falseValue
