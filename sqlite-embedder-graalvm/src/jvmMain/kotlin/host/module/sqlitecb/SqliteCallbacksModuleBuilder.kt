/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.sqlitecb

import org.graalvm.polyglot.Context
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.IndirectFunctionTableIndex
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.functionTable
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.setupImportedEnvMemory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.setupWasmModuleFunctions
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.withWasmContext
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.NodeFactory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.sqlitecb.function.Sqlite3LoggingAdapter
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.sqlitecb.function.Sqlite3ProgressAdapter
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.sqlitecb.function.Sqlite3TraceAdapter
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.GraalvmManagedThreadFactory.Companion.PTHREAD_ROUTINE_CALLBACK_FUNCTION
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.WasmManagedThreadStore
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.function.StartManagedThreadCallbackFunctionAdapter
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmModules.SQLITE3_CALLBACK_MANAGER_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunction

internal class SqliteCallbacksModuleBuilder(
    private val graalContext: Context,
    private val host: EmbedderHost,
    private val callbackStore: SqliteCallbackStore,
    private val managedThreadStore: WasmManagedThreadStore,
) {
    private val callbackHostFunctions: Map<HostFunction, NodeFactory> = mapOf(
        SqliteCallbacksModuleFunction.SQLITE3_TRACE_CALLBACK to {
                language: WasmLanguage,
                module: WasmModule,
                host: EmbedderHost,
            ->
            Sqlite3TraceAdapter(language, module, host, callbackStore.sqlite3TraceCallbacks::get)
        },
        SqliteCallbacksModuleFunction.SQLITE3_PROGRESS_CALLBACK to { language, module, host ->
            Sqlite3ProgressAdapter(language, module, host, callbackStore.sqlite3ProgressCallbacks::get)
        },
        SqliteCallbacksModuleFunction.SQLITE3_LOGGING_CALLBACK to { language, module, host ->
            Sqlite3LoggingAdapter(language, module, host, callbackStore::sqlite3LogCallback)
        },
        PTHREAD_ROUTINE_CALLBACK_FUNCTION to { language, module, host ->
            StartManagedThreadCallbackFunctionAdapter(language, module, host, managedThreadStore)
        }
    )

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
            return setupWasmModuleFunctions(wasmContext, host, module, callbackHostFunctions)
        }
    }

    fun setupIndirectFunctionTable(): GraalvmSqliteCallbackFunctionIndexes = graalContext.withWasmContext { wasmContext ->
        // Ensure module linked
        val moduleInstance: WasmInstance = wasmContext.moduleInstances().getValue(SQLITE3_CALLBACK_MANAGER_MODULE_NAME)
        wasmContext.linker().tryLink(moduleInstance)

        val functionTable = wasmContext.functionTable
        val firstFuncId = functionTable.grow(callbackHostFunctions.size, null)
        val funcIdx: Map<HostFunction, IndirectFunctionTableIndex> = callbackHostFunctions.keys
            .mapIndexed { index, hostFunction ->
                val indirectFuncId = firstFuncId + index
                val funcInstance = moduleInstance.readMember(hostFunction.wasmName)
                functionTable[indirectFuncId] = funcInstance
                hostFunction to IndirectFunctionTableIndex(indirectFuncId)
            }.toMap()
        return GraalvmSqliteCallbackFunctionIndexes(funcIdx)
    }
}
