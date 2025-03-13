/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.chicory.host.module.sqlitecb.function

import at.released.wasm.sqlite.open.helper.WasmPtr
import at.released.wasm.sqlite.open.helper.chicory.ext.asWasmAddr
import at.released.wasm.sqlite.open.helper.embedder.sqlitecb.function.Sqlite3ProgressFunctionHandle
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback
import at.released.weh.host.EmbedderHost
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle

internal class Sqlite3ProgressAdapter(
    host: EmbedderHost,
    progressCallbackStore: (WasmPtr<SqliteDb>) -> SqliteProgressCallback?,
) : WasmFunctionHandle {
    private val handle = Sqlite3ProgressFunctionHandle(host, progressCallbackStore)

    override fun apply(instance: Instance?, vararg args: Long): LongArray {
        val result = handle.execute(args[0].asWasmAddr())
        return longArrayOf(result.toLong())
    }
}
