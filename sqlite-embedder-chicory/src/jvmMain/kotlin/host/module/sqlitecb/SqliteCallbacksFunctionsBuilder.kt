/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb

import at.released.weh.host.EmbedderHost
import at.released.weh.host.base.function.IndirectFunctionTableIndex
import at.released.weh.host.base.memory.ReadOnlyMemory
import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.opcodeToChicory
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function.Sqlite3LoggingAdapter
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function.Sqlite3ProgressAdapter
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function.Sqlite3TraceAdapter
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.SqliteCallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SQLITE3_CALLBACK_MANAGER_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_LOGGING_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_PROGRESS_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_TRACE_CALLBACK

internal class SqliteCallbacksFunctionsBuilder(
    private val memory: ReadOnlyMemory,
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
                sqliteCallbackFunction.type.paramTypes.map(::opcodeToChicory),
                sqliteCallbackFunction.type.returnTypes.map(::opcodeToChicory),
            )
        }
    }

    private class ChicorySqliteCallbackFunctionIndexes(
        functionMap: Map<SqliteCallbacksModuleFunction, IndirectFunctionTableIndex>,
    ) : SqliteCallbackFunctionIndexes {
        override val traceFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_TRACE_CALLBACK)
        override val progressFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_PROGRESS_CALLBACK)
        override val loggingCallbackFunction: IndirectFunctionTableIndex =
            functionMap.getValue(SQLITE3_LOGGING_CALLBACK)
    }

    internal companion object {
        private fun SqliteCallbacksModuleFunction.createFunctionHandle(
            host: EmbedderHost,
            memory: ReadOnlyMemory,
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
        ): SqliteCallbackFunctionIndexes {
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
            return ChicorySqliteCallbackFunctionIndexes(indirectIndexes)
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
