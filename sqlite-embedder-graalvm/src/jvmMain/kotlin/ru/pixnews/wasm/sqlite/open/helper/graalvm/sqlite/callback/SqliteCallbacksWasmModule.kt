/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback

import org.graalvm.polyglot.Context
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.IndirectFunctionTableIndex
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.functionTable
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.setupImportedEnvMemory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.setupWasmModuleFunctions
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.withWasmContext
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.NodeFactory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func.Sqlite3CallExecAdapter
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func.Sqlite3ComparatorAdapter
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func.Sqlite3DestroyComparatorAdapter
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func.Sqlite3LoggingAdapter
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func.Sqlite3ProgressAdapter
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func.Sqlite3TraceAdapter
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost

internal const val SQLITE3_CALLBACK_MANAGER_MODULE_NAME = "sqlite3-callback-manager"

internal class SqliteCallbacksModuleBuilder(
    private val graalContext: Context,
    private val host: SqliteEmbedderHost,
    private val callbackStore: SqliteCallbackStore,
) {
    private val sqliteCallbackHostFunctions: Map<out SqliteCallbacksModuleFunction, NodeFactory> = mapOf(
        SqliteCallbacksModuleFunction.SQLITE3_EXEC_CALLBACK to {
                language: WasmLanguage,
                module: WasmModule,
                host: SqliteEmbedderHost,
                functionName: String,
            ->
            Sqlite3CallExecAdapter(
                language = language,
                module = module,
                execCallbackStore = callbackStore.sqlite3ExecCallbacks::get,
                host = host,
                functionName = functionName,
            )
        },
        SqliteCallbacksModuleFunction.SQLITE3_TRACE_CALLBACK to {
                language: WasmLanguage,
                module: WasmModule,
                host: SqliteEmbedderHost,
                functionName: String,
            ->
            Sqlite3TraceAdapter(
                language = language,
                module = module,
                traceCallbackStore = callbackStore.sqlite3TraceCallbacks::get,
                host = host,
                functionName = functionName,
            )
        },
        SqliteCallbacksModuleFunction.SQLITE3_PROGRESS_CALLBACK to {
                language: WasmLanguage,
                module: WasmModule,
                host: SqliteEmbedderHost,
                functionName: String,
            ->
            Sqlite3ProgressAdapter(
                language = language,
                module = module,
                progressCallbackStore = callbackStore.sqlite3ProgressCallbacks::get,
                host = host,
                functionName = functionName,
            )
        },
        SqliteCallbacksModuleFunction.SQLITE3_COMPARATOR_CALL_CALLBACK to {
                language: WasmLanguage,
                module: WasmModule,
                host: SqliteEmbedderHost,
                functionName: String,
            ->
            Sqlite3ComparatorAdapter(
                language = language,
                module = module,
                comparatorStore = callbackStore.sqlite3Comparators::get,
                host = host,
                functionName = functionName,
            )
        },
        SqliteCallbacksModuleFunction.SQLITE3_DESTROY_COMPARATOR_FUNCTION to {
                language: WasmLanguage,
                module: WasmModule,
                host: SqliteEmbedderHost,
                functionName: String,
            ->
            Sqlite3DestroyComparatorAdapter(
                language = language,
                module = module,
                comparatorStore = callbackStore.sqlite3Comparators,
                host = host,
                functionName = functionName,
            )
        },
        SqliteCallbacksModuleFunction.SQLITE3_LOGGING_CALLBACK to {
                language: WasmLanguage,
                module: WasmModule,
                host: SqliteEmbedderHost,
                functionName: String,
            ->
            Sqlite3LoggingAdapter(
                language = language,
                module = module,
                logCallbackStore = callbackStore::sqlite3LogCallback,
                host = host,
                functionName = functionName,
            )
        },
    ).also {
        check(it.size == SqliteCallbacksModuleFunction.entries.size)
    }

    fun setupModule(
        sharedMemory: Boolean = false,
        useUnsafeMemory: Boolean = false,
    ): WasmInstance {
        val module = WasmModule.create(
            SQLITE3_CALLBACK_MANAGER_MODULE_NAME,
            null,
        )
        graalContext.withWasmContext { wasmContext ->
            module.setupImportedEnvMemory(
                wasmContext,
                sharedMemory = sharedMemory,
                useUnsafeMemory = useUnsafeMemory,
            )
            return setupWasmModuleFunctions(wasmContext, host, module, sqliteCallbackHostFunctions)
        }
    }

    fun setupIndirectFunctionTable(): Sqlite3CallbackFunctionIndexes = graalContext.withWasmContext { wasmContext ->
        // Ensure module linked
        val moduleInstance: WasmInstance = wasmContext.moduleInstances().getValue(SQLITE3_CALLBACK_MANAGER_MODULE_NAME)
        wasmContext.linker().tryLink(moduleInstance)

        val functionTable = wasmContext.functionTable
        val firstFuncId = functionTable.grow(sqliteCallbackHostFunctions.size, null)
        val funcIdx: Map<SqliteCallbacksModuleFunction, IndirectFunctionTableIndex> = sqliteCallbackHostFunctions.keys
            .mapIndexed { index, hostFunction ->
                val indirectFuncId = firstFuncId + index
                val funcInstance = moduleInstance.readMember(hostFunction.wasmName)
                functionTable[indirectFuncId] = funcInstance
                hostFunction to IndirectFunctionTableIndex(indirectFuncId)
            }.toMap()
        return GraalvmSqliteCallbackFunctionIndexes(funcIdx)
    }
}
