/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder.callback

import ru.pixnews.wasm.sqlite.open.helper.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore.SqliteCallbackIdMap
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore.SqliteCallbackMap
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore.SqliteComparatorId
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore.SqliteExecCallbackId
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteComparatorCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteExecCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteLogCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceCallback
import kotlin.concurrent.Volatile

@InternalWasmSqliteHelperApi
internal class DefaultSqliteCallbackStore : SqliteCallbackStore {
    override val sqlite3Comparators: SqliteCallbackIdMap<SqliteComparatorId, SqliteComparatorCallback> =
        ThreadSafeCallbackIdMap(SqliteCallbackStore::SqliteComparatorId)
    override val sqlite3ExecCallbacks: SqliteCallbackIdMap<SqliteExecCallbackId, SqliteExecCallback> =
        ThreadSafeCallbackIdMap(SqliteCallbackStore::SqliteExecCallbackId)
    override val sqlite3TraceCallbacks: SqliteCallbackMap<WasmPtr<SqliteDb>, SqliteTraceCallback> =
        ThreadSafeCallbackMap()
    override val sqlite3ProgressCallbacks: SqliteCallbackMap<WasmPtr<SqliteDb>, SqliteProgressCallback> =
        ThreadSafeCallbackMap()

    @Volatile
    override var sqlite3LogCallback: SqliteLogCallback? = null
}
