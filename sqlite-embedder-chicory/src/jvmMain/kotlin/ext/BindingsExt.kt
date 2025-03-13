/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.chicory.ext

import at.released.wasm.sqlite.open.helper.chicory.exports.ChicoryWasmFunctionBinding
import at.released.wasm.sqlite.open.helper.embedder.exports.SinglePropertyLazyValue
import at.released.weh.wasm.core.WasmFunctionBinding
import com.dylibso.chicory.runtime.ExportFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.ChicoryException
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal fun Instance.functionMember(): ReadOnlyProperty<Any?, WasmFunctionBinding> {
    return object : ReadOnlyProperty<Any?, WasmFunctionBinding> {
        private val binding: SinglePropertyLazyValue<WasmFunctionBinding> = SinglePropertyLazyValue {
            try {
                val export = this@functionMember.export(it)
                ChicoryWasmFunctionBinding(export)
            } catch (ce: ChicoryException) {
                throw IllegalStateException("No member $it", ce)
            }
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): WasmFunctionBinding = binding.get(property)
    }
}

internal fun Instance.optionalFunctionMember(): ReadOnlyProperty<Any?, WasmFunctionBinding?> {
    return object : ReadOnlyProperty<Any?, WasmFunctionBinding?> {
        private val binding: SinglePropertyLazyValue<WasmFunctionBinding?> = SinglePropertyLazyValue {
            try {
                this@optionalFunctionMember.export(it).let(::ChicoryWasmFunctionBinding)
            } catch (@Suppress("SwallowedException") ce: ChicoryException) {
                null
            }
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): WasmFunctionBinding? = binding.get(property)
    }
}

internal fun Instance.intGlobalMember(): ReadWriteProperty<Any?, Int> {
    return object : ReadWriteProperty<Any?, Int> {
        private val binding: SinglePropertyLazyValue<ExportFunction> = SinglePropertyLazyValue {
            try {
                this@intGlobalMember.export(it)
            } catch (ce: ChicoryException) {
                throw IllegalStateException("No member $it", ce)
            }
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            return binding.get(property).apply()[0].toInt()
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            binding.get(property).apply(value.toLong())
        }
    }
}

internal fun Instance.optionalIntGlobalMember(): ReadWriteProperty<Any?, Int?> {
    return object : ReadWriteProperty<Any?, Int?> {
        private val binding = SinglePropertyLazyValue {
            try {
                this@optionalIntGlobalMember.export(it)
            } catch (@Suppress("SwallowedException") ce: ChicoryException) {
                null
            }
        }

        override fun getValue(thisRef: Any?, property: KProperty<*>): Int? {
            return binding.get(property)?.let { it.apply()[0].toInt() }
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int?) {
            requireNotNull(value)
            binding.get(property)?.apply(value.toLong())
        }
    }
}
