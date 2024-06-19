/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder.callback

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteComparatorCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteExecCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteLogCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceCallback

@InternalWasmSqliteHelperApi
public interface SqliteCallbackStore {
    public val sqlite3Comparators: SqliteCallbackIdMap<SqliteComparatorId, SqliteComparatorCallback>
    public val sqlite3ExecCallbacks: SqliteCallbackIdMap<SqliteExecCallbackId, SqliteExecCallback>
    public val sqlite3TraceCallbacks: SqliteCallbackMap<WasmPtr<SqliteDb>, SqliteTraceCallback>
    public val sqlite3ProgressCallbacks: SqliteCallbackMap<WasmPtr<SqliteDb>, SqliteProgressCallback>
    public var sqlite3LogCallback: SqliteLogCallback?

    public interface SqliteCallbackId {
        public val id: Int
    }

    @JvmInline
    public value class SqliteExecCallbackId(override val id: Int) : SqliteCallbackId

    @JvmInline
    public value class SqliteComparatorId(override val id: Int) : SqliteCallbackId

    public interface SqliteCallbackIdMap<K : SqliteCallbackId, V : Any> {
        public fun put(value: V): K
        public operator fun get(key: K): V?
        public fun remove(key: K): V?
    }

    public interface SqliteCallbackMap<K : Any, V : Any> {
        public operator fun get(key: K): V?
        public operator fun set(key: K, value: V): V?
        public fun remove(key: K): V?
    }

    public companion object {
        public operator fun invoke(): SqliteCallbackStore = DefaultSqliteCallbackStore()
    }
}
