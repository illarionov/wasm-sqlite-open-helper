/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder.bindings

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr

@InternalWasmSqliteHelperApi
public interface SqliteMemoryBindings {
    // TODO: prefix
    public fun <P : Any?> allocOrThrow(len: UInt): WasmPtr<P>
    public fun free(ptr: WasmPtr<*>)
}

@InternalWasmSqliteHelperApi
public fun SqliteMemoryBindings.freeSilent(value: WasmPtr<*>): Result<Unit> = kotlin.runCatching {
    free(value)
}
