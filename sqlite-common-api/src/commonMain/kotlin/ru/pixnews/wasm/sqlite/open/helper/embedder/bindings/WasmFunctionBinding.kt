/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder.bindings

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr

@InternalWasmSqliteHelperApi
public interface WasmFunctionBinding {
    // TODO: split to interfaces?
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
