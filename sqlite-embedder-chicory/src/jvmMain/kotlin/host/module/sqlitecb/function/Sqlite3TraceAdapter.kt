/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function

import at.released.weh.host.EmbedderHost
import at.released.weh.wasm.core.memory.ReadOnlyMemory
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function.Sqlite3TraceFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode

internal class Sqlite3TraceAdapter(
    host: EmbedderHost,
    private val memory: ReadOnlyMemory,
    traceCallbackStore: (WasmPtr<SqliteDb>) -> SqliteTraceCallback?,
) : WasmFunctionHandle {
    private val handle = Sqlite3TraceFunctionHandle(host, traceCallbackStore)

    override fun apply(instance: Instance?, vararg args: Long): LongArray {
        val result = handle.execute(
            memory,
            SqliteTraceEventCode(args[0].toUInt()),
            args[1].asWasmAddr(),
            args[2].asWasmAddr(),
            args[3].toInt().toLong(),
        )
        return longArrayOf(result.toLong())
    }
}
