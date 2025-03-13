/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.ext

import at.released.weh.wasm.core.HostFunction.HostFunctionType
import at.released.weh.wasm.core.WasmValueType
import at.released.weh.wasm.core.WasmValueTypes.F32
import at.released.weh.wasm.core.WasmValueTypes.F64
import at.released.weh.wasm.core.WasmValueTypes.I32
import at.released.weh.wasm.core.WasmValueTypes.I64
import io.github.charlietap.chasm.type.NumberType
import io.github.charlietap.chasm.type.ResultType
import io.github.charlietap.chasm.type.ValueType
import io.github.charlietap.chasm.type.FunctionType as ChasmFunctionType

internal fun List<HostFunctionType>.toChasmFunctionTypes(): Map<HostFunctionType, ChasmFunctionType> = associateWith(
    HostFunctionType::toChasmFunctionType,
)

internal fun HostFunctionType.toChasmFunctionType(): ChasmFunctionType = ChasmFunctionType(
    ResultType(paramTypes.map(::toChasmValueTypes)),
    ResultType(returnTypes.map(::toChasmValueTypes)),
)

internal fun toChasmValueTypes(@WasmValueType type: Int): ValueType = when (type) {
    I32 -> ValueType.Number(NumberType.I32)
    I64 -> ValueType.Number(NumberType.I64)
    F32 -> ValueType.Number(NumberType.F32)
    F64 -> ValueType.Number(NumberType.F64)
    else -> error("Unsupported WASM value type `$type`")
}
