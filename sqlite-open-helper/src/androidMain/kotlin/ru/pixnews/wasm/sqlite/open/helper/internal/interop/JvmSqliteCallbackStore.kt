/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.interop

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteComparatorCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteExecCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteLogCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceCallback
import java.util.Collections

internal class JvmSqliteCallbackStore : SqliteCallbackStore {
    override val sqlite3Comparators: JvmIdMap<SqliteCallbackStore.SqliteComparatorId, SqliteComparatorCallback> =
        JvmIdMap(SqliteCallbackStore::SqliteComparatorId)
    override val sqlite3ExecCallbacks: JvmIdMap<SqliteCallbackStore.SqliteExecCallbackId, SqliteExecCallback> =
        JvmIdMap(SqliteCallbackStore::SqliteExecCallbackId)
    override val sqlite3TraceCallbacks: MutableMap<WasmPtr<SqliteDb>, SqliteTraceCallback> =
        Collections.synchronizedMap(mutableMapOf())
    override val sqlite3ProgressCallbacks: MutableMap<WasmPtr<SqliteDb>, SqliteProgressCallback> =
        Collections.synchronizedMap(mutableMapOf())

    @Volatile
    override var sqlite3LogCallback: SqliteLogCallback? = null
}
