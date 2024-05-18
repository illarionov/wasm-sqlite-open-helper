/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.sqlitecb.function

import io.github.charlietap.chasm.executor.runtime.value.ExecutionValue
import io.github.charlietap.chasm.executor.runtime.value.NumberValue.I32
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.EmscriptenHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function.Sqlite3ProgressFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback

internal class Sqlite3ProgressAdapter(
    host: EmbedderHost,
    progressCallbackStore: (WasmPtr<SqliteDb>) -> SqliteProgressCallback?,
) : EmscriptenHostFunctionHandle {
    private val handle = Sqlite3ProgressFunctionHandle(host, progressCallbackStore)

    override fun invoke(args: List<ExecutionValue>): List<ExecutionValue> {
        val result = handle.execute(args[0].asWasmAddr())
        return listOf(I32(result))
    }
}
