/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm

import ru.pixnews.wasm.sqlite.open.helper.common.embedder.EmbedderMemory
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteWasmEnvironment
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.exports.SqliteExports
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes

public class GraalvmSqliteWasmEnvironment internal constructor(
    override val sqliteExports: SqliteExports,
    override val memory: EmbedderMemory,
    override val callbackStore: SqliteCallbackStore,
    override val callbackFunctionIndexes: Sqlite3CallbackFunctionIndexes,
    override val runtimeInstance: GraalvmRuntimeInstance,
) : SqliteWasmEnvironment<GraalvmRuntimeInstance>
