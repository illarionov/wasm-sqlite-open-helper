/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.embedder

import at.released.wasm.sqlite.open.helper.InternalWasmSqliteHelperApi
import at.released.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import at.released.wasm.sqlite.open.helper.embedder.exports.SqliteExports
import at.released.wasm.sqlite.open.helper.embedder.functiontable.SqliteCallbackFunctionIndexes
import at.released.weh.wasm.core.memory.Memory

public interface SqliteEmbedder<E : SqliteEmbedderConfig, I : SqliteRuntime> {
    @InternalWasmSqliteHelperApi
    public fun createRuntime(
        commonConfig: WasmSqliteCommonConfig,
        embedderConfigBuilder: E.() -> Unit,
    ): SqliteRuntimeInternal<I>
}

@InternalWasmSqliteHelperApi
public interface SqliteRuntimeInternal<I : SqliteRuntime> : AutoCloseable {
    public val sqliteExports: SqliteExports
    public val memory: Memory
    public val callbackStore: SqliteCallbackStore
    public val callbackFunctionIndexes: SqliteCallbackFunctionIndexes
    public val runtimeInstance: I
}
