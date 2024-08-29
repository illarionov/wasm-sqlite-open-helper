/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.ext

import at.released.weh.host.EmbedderHost
import at.released.weh.host.base.WasmValueType
import at.released.weh.host.base.function.HostFunction
import at.released.weh.host.base.function.HostFunction.HostFunctionType
import at.released.weh.host.base.function.functionTypes
import org.graalvm.wasm.SymbolTable
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmFunction
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.constants.Sizes
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.NodeFactory

internal fun setupWasmModuleFunctions(
    context: WasmContext,
    host: EmbedderHost,
    module: WasmModule,
    functions: Map<out HostFunction, NodeFactory>,
): WasmInstance {
    val functionTypes: Map<HostFunctionType, Int> = allocateFunctionTypes(module, functions.keys.functionTypes())
    val exportedFunctions: Map<String, WasmFunction> = declareExportedFunctions(module, functionTypes, functions.keys)

    val moduleInstance: WasmInstance = context.readInstance(module)

    functions.forEach { (fn, factory) ->
        val node = factory(context.language(), module, host)
        val exportedIndex = exportedFunctions.getValue(fn.wasmName).index()
        moduleInstance.setTarget(exportedIndex, node.callTarget)
    }
    return moduleInstance
}

internal fun allocateFunctionTypes(
    symbolTable: SymbolTable,
    functionTypes: Collection<HostFunctionType>,
): Map<HostFunctionType, Int> {
    val functionTypeMap: MutableMap<HostFunctionType, Int> = mutableMapOf()
    functionTypes.forEach { type ->
        functionTypeMap.getOrPut(type) {
            val typeIdx = symbolTable.allocateFunctionType(
                type.paramTypes.toTypesByteArray(),
                type.returnTypes.toTypesByteArray(),
                false,
            )
            typeIdx
        }
    }
    return functionTypeMap
}

internal fun List<WasmValueType>.toTypesByteArray(): ByteArray = ByteArray(this.size) {
    requireNotNull(this[it].opcode).toByte()
}

internal fun declareExportedFunctions(
    symbolTable: SymbolTable,
    functionTypes: Map<HostFunctionType, Int>,
    functions: Collection<HostFunction>,
): Map<String, WasmFunction> {
    return functions.associate { fn ->
        val typeIdx = functionTypes.getValue(fn.type)
        val functionIdx = symbolTable.declareExportedFunction(typeIdx, fn.wasmName)
        fn.wasmName to functionIdx
    }
}

internal fun SymbolTable.setupImportedEnvMemory(
    context: WasmContext,
    sharedMemory: Boolean = false,
    useUnsafeMemory: Boolean = false,
    supportMemory64: Boolean = context.contextOptions.supportMemory64(),
) {
    val maxSize: Long
    val is64Bit: Boolean
    if (supportMemory64) {
        maxSize = Sizes.MAX_MEMORY_64_DECLARATION_SIZE
        is64Bit = true
    } else {
        maxSize = @Suppress("MagicNumber") 32768
        is64Bit = false
    }
    val minSize = 0L

    val index = this.memoryCount()
    this.importMemory(
        "env",
        "memory",
        index,
        minSize,
        maxSize,
        is64Bit,
        sharedMemory,
        false,
        useUnsafeMemory,
    )
}
