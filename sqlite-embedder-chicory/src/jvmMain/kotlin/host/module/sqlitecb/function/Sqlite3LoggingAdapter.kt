/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function

import at.released.wasm.sqlite.open.helper.chicory.ext.asWasmAddr
import at.released.wasm.sqlite.open.helper.embedder.sqlitecb.function.Sqlite3LoggingFunctionHandler
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteLogCallback
import at.released.weh.host.EmbedderHost
import at.released.weh.wasm.core.memory.ReadOnlyMemory
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle

internal class Sqlite3LoggingAdapter(
    host: EmbedderHost,
    private val memory: ReadOnlyMemory,
    logCallbackStore: () -> SqliteLogCallback?,
) : WasmFunctionHandle {
    private val handle = Sqlite3LoggingFunctionHandler(host, logCallbackStore)

    override fun apply(instance: Instance?, vararg args: Long): LongArray {
        handle.execute(
            memory,
            // unused context pointer args.getArgAsWasmPtr(0),
            args[1].toInt(),
            args[2].asWasmAddr(),
        )
        return longArrayOf()
    }
}
