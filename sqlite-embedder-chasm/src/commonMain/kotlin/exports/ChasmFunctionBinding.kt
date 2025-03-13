/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.exports

import at.released.weh.bindings.chasm.ext.asInt
import at.released.weh.wasm.core.WasmFunctionBinding
import io.github.charlietap.chasm.embedding.error.ChasmError
import io.github.charlietap.chasm.embedding.invoke
import io.github.charlietap.chasm.embedding.shapes.Instance
import io.github.charlietap.chasm.embedding.shapes.Store
import io.github.charlietap.chasm.embedding.shapes.fold
import io.github.charlietap.chasm.runtime.value.ExecutionValue
import io.github.charlietap.chasm.runtime.value.NumberValue
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asLong
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.orThrow

internal class ChasmFunctionBinding(
    private val store: Store,
    private val instance: Instance,
    private val name: String,
) : WasmFunctionBinding {
    override fun executeVoid(vararg args: Any?) {
        invoke(store, instance, name, args.argsToValues()).orThrow()
    }

    override fun executeForInt(vararg args: Any?): Int = invoke(store, instance, name, args.argsToValues())
        .fold(
            { it[0].asInt() },
            ::throwOnError,
        )

    override fun executeForLong(vararg args: Any?): Long = invoke(store, instance, name, args.argsToValues())
        .fold(
            { it[0].asLong() },
            ::throwOnError,
        )

    override fun executeForFloat(vararg args: Any?): Float = invoke(store, instance, name, args.argsToValues())
        .fold(
            {
                (it[0] as NumberValue.F32).value
            },
            ::throwOnError,
        )

    override fun executeForDouble(vararg args: Any?): Double = invoke(store, instance, name, args.argsToValues())
        .fold(
            {
                (it[0] as NumberValue.F64).value
            },
            ::throwOnError,
        )

    override fun executeForPtr(vararg args: Any?): Int = invoke(store, instance, name, args.argsToValues())
        .fold(
            { it[0].asInt() },
            ::throwOnError,
        )
}

private fun Array<out Any?>.argsToValues(): List<ExecutionValue> {
    return if (this.isEmpty()) {
        emptyList()
    } else {
        List(this.size) { idx ->
            when (val arg = this[idx]) {
                is Int -> NumberValue.I32(arg)
                is UInt -> NumberValue.I32(arg.toInt())
                is Long -> NumberValue.I64(arg.toLong())
                is ULong -> NumberValue.I64(arg.toLong())
                is Float -> NumberValue.F32(arg.toFloat())
                is Double -> NumberValue.F64(arg.toDouble())
                else -> error("Unsupported argument type $arg")
            }
        }
    }
}

private fun throwOnError(error: ChasmError.ExecutionError): Nothing =
    throw ChasmErrorException(error)

private class ChasmErrorException(
    public val error: ChasmError,
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
