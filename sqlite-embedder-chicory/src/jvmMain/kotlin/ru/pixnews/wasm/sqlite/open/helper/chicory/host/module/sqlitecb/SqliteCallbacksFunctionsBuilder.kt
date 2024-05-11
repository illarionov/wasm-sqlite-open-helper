/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.chicory
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function.Sqlite3LoggingAdapter
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function.Sqlite3ProgressAdapter
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function.Sqlite3TraceAdapter
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.IndirectFunctionTableIndex
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_LOGGING_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_PROGRESS_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_TRACE_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmModules.SQLITE3_CALLBACK_MANAGER_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmValueType
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory

internal class SqliteCallbacksFunctionsBuilder(
    private val memory: Memory,
    private val host: EmbedderHost,
    private val callbackStore: SqliteCallbackStore,
) {
    fun asChicoryHostFunctions(
        moduleName: String = SQLITE3_CALLBACK_MANAGER_MODULE_NAME,
    ): List<HostFunction> {
        return SqliteCallbacksModuleFunction.entries.map { sqliteCallbackFunction ->
            HostFunction(
                sqliteCallbackFunction.createFunctionHandle(host, memory, callbackStore),
                moduleName,
                sqliteCallbackFunction.wasmName,
                sqliteCallbackFunction.type.paramTypes.map(WasmValueType::chicory),
                sqliteCallbackFunction.type.returnTypes.map(WasmValueType::chicory),
            )
        }
    }

    private class ChicorySqlite3CallbackFunctionIndexes(
        functionMap: Map<SqliteCallbacksModuleFunction, IndirectFunctionTableIndex>,
    ) : Sqlite3CallbackFunctionIndexes {
        override val traceFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_TRACE_CALLBACK)
        override val progressFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_PROGRESS_CALLBACK)
        override val loggingCallbackFunction: IndirectFunctionTableIndex =
            functionMap.getValue(SQLITE3_LOGGING_CALLBACK)
    }

    internal companion object {
        private fun SqliteCallbacksModuleFunction.createFunctionHandle(
            host: EmbedderHost,
            memory: Memory,
            callbackStore: SqliteCallbackStore,
        ): WasmFunctionHandle = when (this) {
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

        fun setupIndirectFunctionIndexes(
            instance: Instance,
        ): Sqlite3CallbackFunctionIndexes {
            val sqliteFunctionIndexes: List<Pair<SqliteCallbacksModuleFunction, Int>> = sqliteCallbacksFunctionIndexes(
                instance.imports().functions(),
            )

            val funcTable = instance.table(0)
            val indirectIndexBase = funcTable.grow(sqliteFunctionIndexes.size, 0, instance)
            val indirectIndexes: Map<SqliteCallbacksModuleFunction, IndirectFunctionTableIndex> =
                sqliteFunctionIndexes.mapIndexed { index, (function, hostImportFuncId) ->
                    val indirectIndex = IndirectFunctionTableIndex(indirectIndexBase + index)
                    funcTable.setRef(indirectIndex.funcId, hostImportFuncId, instance)
                    function to indirectIndex
                }.toMap()
            return ChicorySqlite3CallbackFunctionIndexes(indirectIndexes)
        }

        private fun sqliteCallbacksFunctionIndexes(
            hostFunctions: Array<HostFunction>,
        ): List<Pair<SqliteCallbacksModuleFunction, Int>> {
            return hostFunctions.mapIndexedNotNull { index, hostFunction ->
                val func = SqliteCallbacksModuleFunction.byWasmName[hostFunction.fieldName()]
                if (func != null) {
                    func to index
                } else {
                    null
                }
            }
        }
    }
}
