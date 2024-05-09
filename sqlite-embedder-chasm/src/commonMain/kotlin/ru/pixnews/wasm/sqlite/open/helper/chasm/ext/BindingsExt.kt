/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.ext

import ru.pixnews.wasm.sqlite.open.helper.chasm.bindings.ChasmFunctionBinding
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.ChasmInstanceBuilder.ChasmInstance
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal fun ChasmInstance.member(): ReadOnlyProperty<Any?, ChasmFunctionBinding> {
    return ReadOnlyProperty { _, prop: KProperty<*> ->
        instance.exports.find { it.name.name == prop.name } ?: error("Property ${prop.name} not found")
        ChasmFunctionBinding(store, instance, prop.name)
    }
}

internal fun ChasmInstance.memberIfExists(): ReadOnlyProperty<Any?, ChasmFunctionBinding?> {
    return ReadOnlyProperty { _, prop: KProperty<*> ->
        val export = instance.exports.find { it.name.name == prop.name }
        if (export != null) {
            ChasmFunctionBinding(store, instance, prop.name)
        } else {
            null
        }
    }
}
