/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host

import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.I32

internal class HostFunction(
    val name: String,
    val type: HostFunctionType,
    val nodeFactory: NodeFactory = notImplementedFunctionNodeFactory,
) {
    constructor(
        name: String,
        paramTypes: List<WasmValueType>,
        retTypes: List<WasmValueType> = listOf(),
        nodeFactory: NodeFactory = notImplementedFunctionNodeFactory,
    ) : this(name, HostFunctionType(paramTypes, retTypes), nodeFactory)
}

internal data class HostFunctionType(
    val params: List<WasmValueType>,
    val returnTypes: List<WasmValueType> = listOf(),
)

internal typealias NodeFactory = (
    language: WasmLanguage,
    instance: WasmInstance,
    host: SqliteEmbedderHost,
    functionName: String,
) -> BaseWasmNode

internal fun MutableList<HostFunction>.fn(
    name: String,
    paramTypes: List<WasmValueType>,
    retType: WasmValueType = I32,
    nodeFactory: NodeFactory = notImplementedFunctionNodeFactory,
) = add(HostFunction(name, paramTypes, listOf(retType), nodeFactory))

internal fun MutableList<HostFunction>.fnVoid(
    name: String,
    paramTypes: List<WasmValueType>,
    nodeFactory: NodeFactory = notImplementedFunctionNodeFactory,
) = add(HostFunction(name, paramTypes, emptyList(), nodeFactory))
