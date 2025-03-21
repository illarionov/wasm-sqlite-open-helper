/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.chicory.host.module.sqlitecb

import at.released.wasm.sqlite.open.helper.chicory.ext.opcodeToChicory
import at.released.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function.Sqlite3LoggingAdapter
import at.released.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function.Sqlite3ProgressAdapter
import at.released.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function.Sqlite3TraceAdapter
import at.released.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import at.released.wasm.sqlite.open.helper.embedder.functiontable.IndirectFunctionTableIndex
import at.released.wasm.sqlite.open.helper.embedder.functiontable.SqliteCallbackFunctionIndexes
import at.released.wasm.sqlite.open.helper.embedder.sqlitecb.SQLITE3_CALLBACK_MANAGER_MODULE_NAME
import at.released.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import at.released.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_LOGGING_CALLBACK
import at.released.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_PROGRESS_CALLBACK
import at.released.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_TRACE_CALLBACK
import at.released.weh.host.EmbedderHost
import at.released.weh.wasm.core.memory.ReadOnlyMemory
import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.ImportFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle

internal class SqliteCallbacksFunctionsBuilder(
    private val memory: ReadOnlyMemory,
    private val host: EmbedderHost,
    private val callbackStore: SqliteCallbackStore,
) {
    fun asChicoryHostFunctions(
        moduleName: String = SQLITE3_CALLBACK_MANAGER_MODULE_NAME,
    ): List<HostFunction> = SqliteCallbacksModuleFunction.entries.map { sqliteCallbackFunction ->
        HostFunction(
            moduleName,
            sqliteCallbackFunction.wasmName,
            sqliteCallbackFunction.type.paramTypes.map(::opcodeToChicory),
            sqliteCallbackFunction.type.returnTypes.map(::opcodeToChicory),
            sqliteCallbackFunction.createFunctionHandle(host, memory, callbackStore),
        )
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
            hostFunctions: Array<ImportFunction>,
        ): List<Pair<SqliteCallbacksModuleFunction, Int>> {
            return hostFunctions.mapIndexedNotNull { index, hostFunction ->
                val func = SqliteCallbacksModuleFunction.byWasmName[hostFunction.name()]
                if (func != null) {
                    func to index
                } else {
                    null
                }
            }
        }
    }
}
