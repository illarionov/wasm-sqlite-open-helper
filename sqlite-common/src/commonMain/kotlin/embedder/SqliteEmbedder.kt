/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder

import at.released.weh.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.exports.SqliteExports
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.SqliteCallbackFunctionIndexes

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
