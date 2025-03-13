/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper

import ru.pixnews.wasm.sqlite.open.helper.WasmPtr.Companion.C_NULL
import kotlin.jvm.JvmInline

@JvmInline
public value class WasmPtr<out P : Any?>(
    public val addr: Int,
) {
    override fun toString(): String = "0x${addr.toString(16)}"

    @Suppress("UNCHECKED_CAST")
    @InternalWasmSqliteHelperApi
    public companion object {
        public const val WASM_SIZEOF_PTR: UInt = 4U
        public val C_NULL: WasmPtr<*> = WasmPtr<Unit>(0)
        public fun <P> cNull(): WasmPtr<P> = C_NULL as WasmPtr<P>
    }
}

@InternalWasmSqliteHelperApi
public fun WasmPtr<*>.isNull(): Boolean = this == C_NULL

@InternalWasmSqliteHelperApi
public operator fun <P> WasmPtr<P>.plus(bytes: Int): WasmPtr<P> = WasmPtr(addr + bytes)
