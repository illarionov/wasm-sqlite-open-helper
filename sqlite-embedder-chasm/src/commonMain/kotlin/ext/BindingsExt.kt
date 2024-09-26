/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.ext

import at.released.weh.wasm.core.WasmFunctionBinding
import io.github.charlietap.chasm.embedding.exports
import ru.pixnews.wasm.sqlite.open.helper.chasm.exports.ChasmFunctionBinding
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.ChasmInstanceBuilder.ChasmInstance
import ru.pixnews.wasm.sqlite.open.helper.embedder.exports.SinglePropertyLazyValue
import kotlin.concurrent.Volatile
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal fun ChasmInstance.functionMember(): ReadOnlyProperty<Any?, WasmFunctionBinding> {
    return object : ReadOnlyProperty<Any?, WasmFunctionBinding> {
        @Volatile
        private var binding: SinglePropertyLazyValue<WasmFunctionBinding> = SinglePropertyLazyValue { propertyName ->
            exports(instance).find { it.name == propertyName }
                ?: error("Property $propertyName not found")
            ChasmFunctionBinding(store, instance, propertyName)
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): WasmFunctionBinding {
            return binding.get(property)
        }
    }
}
