/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr.Companion.WASM_SIZEOF_PTR
import ru.pixnews.wasm.sqlite.open.helper.common.api.plus
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readZeroTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore.SqliteExecCallbackId
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.memory.readPtr
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteExecCallback

public class SqliteExecCallbackFunctionHandle(
    host: SqliteEmbedderHost,
    private val execCallbackStore: (SqliteExecCallbackId) -> SqliteExecCallback?,

) : HostFunctionHandle(SqliteCallbacksModuleFunction.SQLITE3_EXEC_CALLBACK, host) {
    public fun execute(
        memory: Memory,
        arg1: Int,
        columns: Int,
        pResults: WasmPtr<WasmPtr<Byte>>,
        pColumnNames: WasmPtr<WasmPtr<Byte>>,
    ): Int {
        logger.v { "Calling exec callback arg1: $arg1 columns: $columns names: $pColumnNames results: $pResults" }
        val delegateId = SqliteExecCallbackId(arg1)
        val delegate = execCallbackStore(delegateId) ?: error("Callback $delegateId not registered")

        val columnNames = (0 until columns).map { columnNo ->
            val ptr: WasmPtr<Byte> = memory.readPtr(pColumnNames + (columnNo * WASM_SIZEOF_PTR.toInt()))
            memory.readZeroTerminatedString(ptr)
        }

        val results = (0 until columns).map { columnNo ->
            val ptr: WasmPtr<Byte> = memory.readPtr(pResults + (columnNo * WASM_SIZEOF_PTR.toInt()))
            memory.readZeroTerminatedString(ptr)
        }
        return delegate(columnNames, results)
    }
}
