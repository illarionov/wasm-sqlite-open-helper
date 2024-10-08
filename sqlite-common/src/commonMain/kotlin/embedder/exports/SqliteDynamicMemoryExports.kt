/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder.exports

import ru.pixnews.wasm.sqlite.open.helper.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi

@InternalWasmSqliteHelperApi
public interface SqliteDynamicMemoryExports {
    public fun <P : Any?> sqliteAllocOrThrow(len: UInt): WasmPtr<P>
    public fun sqliteFree(ptr: WasmPtr<*>)
}

@InternalWasmSqliteHelperApi
public fun SqliteDynamicMemoryExports.sqliteFreeSilent(value: WasmPtr<*>): Result<Unit> = kotlin.runCatching {
    sqliteFree(value)
}
