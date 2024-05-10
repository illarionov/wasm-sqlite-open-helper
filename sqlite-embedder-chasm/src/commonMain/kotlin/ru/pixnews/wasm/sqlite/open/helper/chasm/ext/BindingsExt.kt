/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.ext

import ru.pixnews.wasm.sqlite.open.helper.chasm.bindings.ChasmFunctionBinding
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.ChasmInstanceBuilder.ChasmInstance
import ru.pixnews.wasm.sqlite.open.helper.host.base.BINDING_NOT_INITIALIZED
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmFunctionBinding
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal fun ChasmInstance.member(): ReadOnlyProperty<Any?, WasmFunctionBinding> {
    return object : ReadOnlyProperty<Any?, WasmFunctionBinding> {
        @Volatile
        private var binding: WasmFunctionBinding = BINDING_NOT_INITIALIZED

        override fun getValue(thisRef: Any?, property: KProperty<*>): WasmFunctionBinding {
            return binding.let { cachedInstance ->
                if (cachedInstance != BINDING_NOT_INITIALIZED) {
                    cachedInstance
                } else {
                    instance.exports.find { it.name.name == property.name }
                        ?: error("Property ${property.name} not found")
                    ChasmFunctionBinding(store, instance, property.name).also {
                        this.binding = it
                    }
                }
            }
        }
    }
}

internal fun ChasmInstance.memberIfExists(): ReadOnlyProperty<Any?, WasmFunctionBinding?> {
    return object : ReadOnlyProperty<Any?, WasmFunctionBinding?> {
        @Volatile
        private var binding: WasmFunctionBinding? = BINDING_NOT_INITIALIZED

        override fun getValue(thisRef: Any?, property: KProperty<*>): WasmFunctionBinding? {
            return binding.let { cachedInstance ->
                if (cachedInstance != BINDING_NOT_INITIALIZED) {
                    cachedInstance
                } else {
                    val export = instance.exports.find { it.name.name == property.name }
                    val newValue = if (export != null) {
                        ChasmFunctionBinding(store, instance, property.name)
                    } else {
                        null
                    }
                    newValue.also {
                        this.binding = it
                    }
                }
            }
        }
    }
}
