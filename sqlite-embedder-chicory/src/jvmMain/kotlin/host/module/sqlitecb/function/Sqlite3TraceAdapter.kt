/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function.Sqlite3TraceFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode

internal class Sqlite3TraceAdapter(
    host: EmbedderHost,
    private val memory: Memory,
    traceCallbackStore: (WasmPtr<SqliteDb>) -> SqliteTraceCallback?,
) : WasmFunctionHandle {
    private val handle = Sqlite3TraceFunctionHandle(host, traceCallbackStore)

    override fun apply(instance: Instance, vararg args: Value): Array<Value> {
        val result = handle.execute(
            memory,
            SqliteTraceEventCode(args[0].asUInt().toUInt()),
            args[1].asWasmAddr(),
            args[2].asWasmAddr(),
            args[3].asInt().toLong(),
        )
        return arrayOf(Value.i32(result.toLong()))
    }
}
