/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore.SqliteComparatorId
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function.SqliteDestroyComparatorFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteComparatorCallback

internal class Sqlite3DestroyComparatorAdapter(
    host: SqliteEmbedderHost,
    comparatorStore: SqliteCallbackStore.SqliteCallbackIdMap<SqliteComparatorId, SqliteComparatorCallback>,
) : WasmFunctionHandle {
    private val handle = SqliteDestroyComparatorFunctionHandle(host, comparatorStore)

    override fun apply(instance: Instance, vararg args: Value): Array<Value> {
        handle.execute(args[0].asInt())
        return emptyArray()
    }
}
