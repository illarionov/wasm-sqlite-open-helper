/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback

import org.graalvm.polyglot.Context
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import ru.pixnews.sqlite.open.helper.graalvm.ext.functionTable
import ru.pixnews.sqlite.open.helper.graalvm.ext.setupWasmModuleFunctions
import ru.pixnews.sqlite.open.helper.graalvm.ext.withWasmContext
import ru.pixnews.sqlite.open.helper.graalvm.host.Host
import ru.pixnews.sqlite.open.helper.graalvm.host.HostFunction
import ru.pixnews.sqlite.open.helper.graalvm.host.fn
import ru.pixnews.sqlite.open.helper.graalvm.host.fnVoid
import ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.func.SQLITE3_COMPARATOR_CALL_FUNCTION_NAME
import ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.func.SQLITE3_DESTROY_COMPARATOR_FUNCTION_NAME
import ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.func.SQLITE3_EXEC_CB_FUNCTION_NAME
import ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.func.SQLITE3_PROGRESS_CB_FUNCTION_NAME
import ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.func.SQLITE3_TRACE_CB_FUNCTION_NAME
import ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.func.Sqlite3CallExecAdapter
import ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.func.Sqlite3ComparatorAdapter
import ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.func.Sqlite3DestroyComparatorAdapter
import ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.func.Sqlite3ProgressAdapter
import ru.pixnews.sqlite.open.helper.graalvm.sqlite.callback.func.Sqlite3TraceAdapter
import ru.pixnews.sqlite.open.helper.host.POINTER
import ru.pixnews.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.sqlite.open.helper.host.functiontable.IndirectFunctionTableIndex
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.WasiValueTypes.U32

internal const val SQLITE3_CALLBACK_MANAGER_MODULE_NAME = "sqlite3-callback-manager"

internal class SqliteCallbacksModuleBuilder(
    private val graalContext: Context,
    private val host: Host,
    private val callbackStore: Sqlite3CallbackStore,
) {
    private val sqliteCallbackHostFunctions: List<HostFunction> = buildList {
        fn(
            name = SQLITE3_EXEC_CB_FUNCTION_NAME,
            paramTypes = listOf(I32, I32, I32, I32),
            retType = I32,
            nodeFactory = { language: WasmLanguage, instance: WasmInstance, _: Host, functionName: String ->
                Sqlite3CallExecAdapter(
                    language = language,
                    instance = instance,
                    callbackStore = callbackStore,
                    functionName = functionName,
                )
            },
        )
        fn(
            name = SQLITE3_TRACE_CB_FUNCTION_NAME,
            paramTypes = listOf(U32, POINTER, POINTER, I32),
            retType = I32,
            nodeFactory = { language: WasmLanguage, instance: WasmInstance, _: Host, functionName: String ->
                Sqlite3TraceAdapter(
                    language = language,
                    instance = instance,
                    callbackStore = callbackStore,
                    functionName = functionName,
                )
            },
        )
        fn(
            name = SQLITE3_PROGRESS_CB_FUNCTION_NAME,
            paramTypes = listOf(POINTER),
            retType = I32,
            nodeFactory = { language: WasmLanguage, instance: WasmInstance, _: Host, functionName: String ->
                Sqlite3ProgressAdapter(
                    language = language,
                    instance = instance,
                    callbackStore = callbackStore,
                    functionName = functionName,
                )
            },
        )
        fn(
            name = SQLITE3_COMPARATOR_CALL_FUNCTION_NAME,
            paramTypes = listOf(I32, I32, POINTER, I32, POINTER),
            retType = I32,
            nodeFactory = { language: WasmLanguage, instance: WasmInstance, _: Host, functionName: String ->
                Sqlite3ComparatorAdapter(
                    language = language,
                    instance = instance,
                    callbackStore = callbackStore,
                    functionName = functionName,
                )
            },
        )
        fnVoid(
            name = SQLITE3_DESTROY_COMPARATOR_FUNCTION_NAME,
            paramTypes = listOf(I32),
            nodeFactory = { language: WasmLanguage, instance: WasmInstance, _: Host, functionName: String ->
                Sqlite3DestroyComparatorAdapter(
                    language = language,
                    instance = instance,
                    callbackStore = callbackStore,
                    functionName = functionName,
                )
            },
        )
    }

    fun setupModule(): WasmInstance {
        val module = WasmModule.create(
            SQLITE3_CALLBACK_MANAGER_MODULE_NAME,
            null,
        )
        graalContext.withWasmContext { wasmContext ->
            return setupWasmModuleFunctions(wasmContext, host, module, sqliteCallbackHostFunctions)
        }
    }

    fun setupIndirectFunctionTable(): Sqlite3CallbackFunctionIndexes = graalContext.withWasmContext { wasmContext ->
        // Ensure module linked
        val moduleInstance: WasmInstance = wasmContext.moduleInstances().getValue(SQLITE3_CALLBACK_MANAGER_MODULE_NAME)
        wasmContext.linker().tryLink(moduleInstance)

        val functionTable = wasmContext.functionTable
        val firstFuncId = functionTable.grow(sqliteCallbackHostFunctions.size, null)
        val funcIdx: Map<String, IndirectFunctionTableIndex> = sqliteCallbackHostFunctions
            .mapIndexed { index, hostFunction ->
                val indirectFuncId = firstFuncId + index
                val funcName = hostFunction.name
                val funcInstance = moduleInstance.readMember(funcName)
                functionTable[indirectFuncId] = funcInstance
                funcName to IndirectFunctionTableIndex(indirectFuncId)
            }.toMap()
        return Sqlite3CallbackFunctionIndexes(funcIdx)
    }
}
