/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.EmbedderMemory
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes

public interface SqliteEmbedder<E : SqliteEmbedderConfig> {
    @InternalWasmSqliteHelperApi
    public fun createSqliteWasmEnvironment(
        commonConfig: WasmSqliteCommonConfig,
        callbackStore: SqliteCallbackStore,
        embedderConfigBuilder: E.() -> Unit,
    ): SqliteWasmEnvironment
}

@InternalWasmSqliteHelperApi
public interface SqliteWasmEnvironment {
    public val sqliteBindings: SqliteBindings
    public val embedderInfo: SQLiteEmbedderRuntimeInfo
    public val memory: EmbedderMemory
    public val callbackFunctionIndexes: Sqlite3CallbackFunctionIndexes
}