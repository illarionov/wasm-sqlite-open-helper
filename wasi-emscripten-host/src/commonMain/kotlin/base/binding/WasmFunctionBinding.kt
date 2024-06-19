/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.base.binding

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr

@InternalWasmSqliteHelperApi
public interface WasmFunctionBinding {
    public fun executeVoid(vararg args: Any?)
    public fun executeForInt(vararg args: Any?): Int
    public fun executeForLong(vararg args: Any?): Long
    public fun executeForFloat(vararg args: Any?): Float
    public fun executeForDouble(vararg args: Any?): Double
    public fun <P : Any?> executeForPtr(vararg args: Any?): WasmPtr<P>
}

@InternalWasmSqliteHelperApi
public fun WasmFunctionBinding.executeForUInt(vararg args: Any?): UInt = executeForInt(args).toUInt()

@InternalWasmSqliteHelperApi
public fun WasmFunctionBinding.executeForULong(vararg args: Any?): ULong = executeForLong(args).toULong()

@InternalWasmSqliteHelperApi
public val BINDING_NOT_INITIALIZED: WasmFunctionBinding = object : WasmFunctionBinding {
    override fun executeVoid(vararg args: Any?) = error("Not initialized")
    override fun executeForInt(vararg args: Any?): Int = error("Not initialized")
    override fun executeForLong(vararg args: Any?): Long = error("Not initialized")
    override fun executeForFloat(vararg args: Any?): Float = error("Not initialized")
    override fun executeForDouble(vararg args: Any?): Double = error("Not initialized")
    override fun <P> executeForPtr(vararg args: Any?): WasmPtr<P> = error("Not initialized")
}
