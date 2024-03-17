/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.common.api

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr.Companion
import kotlin.jvm.JvmInline

@InternalWasmSqliteHelperApi
@JvmInline
public value class WasmPtr<out P : Any?>(
    public val addr: Int,
) {
    @Suppress("MagicNumber")
    override fun toString(): String = "0x${addr.toString(16)}"

    @Suppress("UNCHECKED_CAST")
    @InternalWasmSqliteHelperApi
    public companion object {
        public const val WASM_SIZEOF_PTR: UInt = 4U
        public val SQLITE3_NULL: WasmPtr<*> = WasmPtr<Unit>(0)
        public fun <P> sqlite3Null(): WasmPtr<P> = SQLITE3_NULL as WasmPtr<P>
    }
}

@InternalWasmSqliteHelperApi
public fun WasmPtr<*>.isSqlite3Null(): Boolean = this == Companion.SQLITE3_NULL

@InternalWasmSqliteHelperApi
public operator fun <P> WasmPtr<P>.plus(bytes: Int): WasmPtr<P> = WasmPtr(addr + bytes)
