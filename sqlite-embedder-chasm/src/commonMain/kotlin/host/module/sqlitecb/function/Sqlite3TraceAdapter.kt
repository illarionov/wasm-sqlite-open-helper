/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.chasm.host.module.sqlitecb.function

import at.released.wasm.sqlite.open.helper.WasmPtr
import at.released.wasm.sqlite.open.helper.chasm.ext.asInt
import at.released.wasm.sqlite.open.helper.chasm.ext.asUInt
import at.released.wasm.sqlite.open.helper.chasm.ext.asWasmAddr
import at.released.wasm.sqlite.open.helper.embedder.sqlitecb.function.Sqlite3TraceFunctionHandle
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceCallback
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceEventCode
import at.released.weh.host.EmbedderHost
import at.released.weh.wasm.core.memory.ReadOnlyMemory
import io.github.charlietap.chasm.runtime.value.NumberValue
import io.github.charlietap.chasm.embedding.shapes.HostFunction as ChasmHostFunction

internal class Sqlite3TraceAdapter(
    host: EmbedderHost,
    private val memory: ReadOnlyMemory,
    traceCallbackStore: (WasmPtr<SqliteDb>) -> SqliteTraceCallback?,
) {
    private val handle = Sqlite3TraceFunctionHandle(host, traceCallbackStore)
    val function: ChasmHostFunction = { args ->
        val result = handle.execute(
            memory,
            SqliteTraceEventCode(args[0].asUInt()),
            args[1].asWasmAddr(),
            args[2].asWasmAddr(),
            args[3].asInt().toLong(),
        )
        listOf(NumberValue.I32(result))
    }
}
