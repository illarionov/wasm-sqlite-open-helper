/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb

import ru.pixnews.wasm.sqlite.open.helper.host.base.POINTER
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmValueType
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunction.HostFunctionType
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.WasiValueTypes.U32

/**
 * Implementation of host functions that are called from SQLite callbacks.
 * Keep in sync with callbacks-wasm.c
 */
public enum class SqliteCallbacksModuleFunction(
    public override val wasmName: String,
    public override val type: HostFunctionType,
) : HostFunction {
    SQLITE3_EXEC_CALLBACK(
        wasmName = "ext_sqlite3_exec_cb",
        paramTypes = listOf(I32, I32, POINTER, POINTER),
    ),
    SQLITE3_TRACE_CALLBACK(
        wasmName = "ext_sqlite3_trace_cb",
        paramTypes = listOf(U32, POINTER, POINTER, I32),
        retType = I32,
    ),
    SQLITE3_PROGRESS_CALLBACK(
        wasmName = "ext_sqlite3_progress_cb",
        paramTypes = listOf(POINTER),
        retType = I32,
    ),
    SQLITE3_COMPARATOR_CALL_CALLBACK(
        wasmName = "ext_sqlite3_comparator_call_cb",
        paramTypes = listOf(I32, I32, POINTER, I32, POINTER),
        retType = I32,
    ),
    SQLITE3_DESTROY_COMPARATOR_FUNCTION(
        wasmName = "ext_sqlite3_comparator_destroy",
        paramTypes = listOf(I32),
    ),
    SQLITE3_LOGGING_CALLBACK(
        wasmName = "ext_sqlite3_logging_cb",
        paramTypes = listOf(I32, POINTER, I32),
    ),
    ;

    constructor(
        wasmName: String,
        paramTypes: List<WasmValueType>,
        retType: WasmValueType? = null,
    ) : this(
        wasmName = wasmName,
        type = HostFunctionType(
            params = paramTypes,
            returnTypes = if (retType != null) {
                listOf(retType)
            } else {
                emptyList()
            },
        ),
    )

    public companion object {
        public val byWasmName: Map<String, SqliteCallbacksModuleFunction> = SqliteCallbacksModuleFunction.entries
            .associateBy(SqliteCallbacksModuleFunction::wasmName)
    }
}
