/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.sqlitecb.function

import io.github.charlietap.chasm.executor.runtime.value.ExecutionValue
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asInt
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.EmscriptenHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore.SqliteComparatorId
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function.SqliteDestroyComparatorFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteComparatorCallback

internal class Sqlite3DestroyComparatorAdapter(
    host: EmbedderHost,
    comparatorStore: SqliteCallbackStore.SqliteCallbackIdMap<SqliteComparatorId, SqliteComparatorCallback>,
) : EmscriptenHostFunctionHandle {
    private val handle = SqliteDestroyComparatorFunctionHandle(host, comparatorStore)

    override fun invoke(args: List<ExecutionValue>): List<ExecutionValue> {
        handle.execute(args[0].asInt())
        return emptyList()
    }
}
