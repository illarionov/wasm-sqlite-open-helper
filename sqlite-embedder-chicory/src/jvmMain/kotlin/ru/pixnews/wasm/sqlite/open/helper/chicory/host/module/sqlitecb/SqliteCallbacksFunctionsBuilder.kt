/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.FunctionImport
import com.dylibso.chicory.wasm.types.FunctionType
import com.dylibso.chicory.wasm.types.TypeSection
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.chicory
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function.Sqlite3CallExecAdapter
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function.Sqlite3ComparatorAdapter
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function.Sqlite3DestroyComparatorAdapter
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function.Sqlite3LoggingAdapter
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function.Sqlite3ProgressAdapter
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function.Sqlite3TraceAdapter
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.IndirectFunctionTableIndex
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_COMPARATOR_CALL_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_DESTROY_COMPARATOR_FUNCTION
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_EXEC_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_LOGGING_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_PROGRESS_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_TRACE_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import com.dylibso.chicory.wasm.Module as ChicoryWasmModule

internal class SqliteCallbacksFunctionsBuilder(
    private val memory: Memory,
    private val host: SqliteEmbedderHost,
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
        override val execCallbackFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_EXEC_CALLBACK)
        override val traceFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_TRACE_CALLBACK)
        override val progressFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_PROGRESS_CALLBACK)
        override val comparatorFunction: IndirectFunctionTableIndex = functionMap.getValue(
            SQLITE3_COMPARATOR_CALL_CALLBACK,
        )
        override val destroyComparatorFunction: IndirectFunctionTableIndex = functionMap.getValue(
            SQLITE3_DESTROY_COMPARATOR_FUNCTION,
        )
        override val loggingCallbackFunction: IndirectFunctionTableIndex =
            functionMap.getValue(SQLITE3_LOGGING_CALLBACK)
    }

    internal companion object {
        private const val SQLITE3_CALLBACK_MANAGER_MODULE_NAME = "sqlite3-callback-manager"
        private fun SqliteCallbacksModuleFunction.createFunctionHandle(
            host: SqliteEmbedderHost,
            memory: Memory,
            callbackStore: SqliteCallbackStore,
        ): WasmFunctionHandle = when (this) {
            SQLITE3_EXEC_CALLBACK -> Sqlite3CallExecAdapter(
                host,
                memory,
                callbackStore.sqlite3ExecCallbacks::get,
            )
            SQLITE3_TRACE_CALLBACK -> Sqlite3TraceAdapter(
                host,
                memory,
                callbackStore.sqlite3TraceCallbacks::get,
            )
            SQLITE3_PROGRESS_CALLBACK -> Sqlite3ProgressAdapter(
                host,
                callbackStore.sqlite3ProgressCallbacks::get,
            )
            SQLITE3_COMPARATOR_CALL_CALLBACK -> Sqlite3ComparatorAdapter(
                host,
                memory,
                callbackStore.sqlite3Comparators::get,
            )
            SQLITE3_DESTROY_COMPARATOR_FUNCTION -> Sqlite3DestroyComparatorAdapter(
                host,
                callbackStore.sqlite3Comparators,
            )
            SQLITE3_LOGGING_CALLBACK -> Sqlite3LoggingAdapter(
                host,
                memory,
                callbackStore::sqlite3LogCallback,
            )
        }

        fun addSqliteCallbacksImportsToWasmModule(
            module: ChicoryWasmModule,
            sqliteCallbackHostFunctions: List<HostFunction>
        ) {
//            val sqliteFunctionTypes = registerFunctionTypes(module, sqliteCallbackHostFunctions)
//
//            val importSection = module.importSection()
//            sqliteCallbackHostFunctions.forEach {
//                val type = it.functionType()
//                val typeIndex = sqliteFunctionTypes[type] ?: error("No type $type")
//                importSection.addImport(FunctionImport(it.moduleName(), it.fieldName(), typeIndex))
//            }
        }

        private fun registerFunctionTypes(
            module: ChicoryWasmModule,
            sqliteCallbackHostFunctions: List<HostFunction>
        ): Map<FunctionType, Int> {
            val typeSection: TypeSection = module.typeSection()
            val registeredTypes = typeSection.types()
                .mapIndexed { index, functionType -> functionType to index }
                .toMap()

            return sqliteCallbackHostFunctions.map { function ->
                val type = function.functionType()
                val typeIndex = registeredTypes[type] ?: typeSection.addFunctionType(type)
                type to typeIndex
            }.toMap()
        }

        private fun HostFunction.functionType(): FunctionType = FunctionType.of(
            paramTypes(),
            returnTypes(),
        )

        fun setupIndirectFunctionIndexes(
            instance: Instance,
        ): Sqlite3CallbackFunctionIndexes {
            val sqliteFunctionIndexes: List<Pair<SqliteCallbacksModuleFunction, Int>> =
                instance.imports().functions().sqliteCallbacksFunctionIndexes()

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

        private fun Array<HostFunction>.sqliteCallbacksFunctionIndexes(): List<Pair<SqliteCallbacksModuleFunction, Int>> {
            return mapIndexedNotNull { index, hostFunction ->
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
