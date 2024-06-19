/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.ext

import io.github.charlietap.chasm.executor.runtime.value.ExecutionValue
import io.github.charlietap.chasm.executor.runtime.value.NumberValue.I32
import io.github.charlietap.chasm.executor.runtime.value.NumberValue.I64
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr

internal fun ExecutionValue.asInt(): Int = (this as I32).value
internal fun ExecutionValue.asUInt(): UInt = (this as I32).value.toUInt()
internal fun ExecutionValue.asLong(): Long = (this as I64).value
internal fun ExecutionValue.asULong(): ULong = (this as I64).value.toULong()

internal fun <P : Any?> ExecutionValue.asWasmAddr(): WasmPtr<P> = WasmPtr(asInt())
