/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.sqlitecb

import io.github.charlietap.chasm.embedding.function
import io.github.charlietap.chasm.executor.runtime.store.Store
import io.github.charlietap.chasm.import.Import
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.toChasmFunctionTypes
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.memory.ChasmMemoryAdapter
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.EmscriptenHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.sqlitecb.function.Sqlite3LoggingAdapter
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.sqlitecb.function.Sqlite3ProgressAdapter
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.sqlitecb.function.Sqlite3TraceAdapter
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_LOGGING_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_PROGRESS_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_TRACE_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmModules.SQLITE3_CALLBACK_MANAGER_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory

internal fun getSqliteCallbacksHostFunctions(
    store: Store,
    memory: ChasmMemoryAdapter,
    host: EmbedderHost,
    callbackStore: SqliteCallbackStore,
    moduleName: String = SQLITE3_CALLBACK_MANAGER_MODULE_NAME,
): List<Import> {
    val functionTypes = SqliteCallbacksModuleFunction.entries
        .map(SqliteCallbacksModuleFunction::type)
        .toChasmFunctionTypes()
    return SqliteCallbacksModuleFunction.entries.map { emscriptenFunc ->
        Import(
            moduleName = moduleName,
            entityName = emscriptenFunc.wasmName,
            value = function(
                store = store,
                type = functionTypes.getValue(emscriptenFunc.type),
                function = emscriptenFunc.createChasmHostFunction(host, memory, callbackStore),
            ),
        )
    }
}

private fun SqliteCallbacksModuleFunction.createChasmHostFunction(
    host: EmbedderHost,
    memory: Memory,
    callbackStore: SqliteCallbackStore,
): EmscriptenHostFunctionHandle = when (this) {
    SQLITE3_TRACE_CALLBACK -> Sqlite3TraceAdapter(
        host,
        memory,
        callbackStore.sqlite3TraceCallbacks::get,
    )

    SQLITE3_PROGRESS_CALLBACK -> Sqlite3ProgressAdapter(
        host,
        callbackStore.sqlite3ProgressCallbacks::get,
    )

    SQLITE3_LOGGING_CALLBACK -> Sqlite3LoggingAdapter(
        host,
        memory,
        callbackStore::sqlite3LogCallback,
    )
}
