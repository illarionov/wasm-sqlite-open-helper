/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.sqlitecb.function

import at.released.weh.host.EmbedderHost
import at.released.weh.host.base.WasmPtr
import io.github.charlietap.chasm.embedding.shapes.Value
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function.Sqlite3ProgressFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback
import io.github.charlietap.chasm.embedding.shapes.HostFunction as ChasmHostFunction

internal class Sqlite3ProgressAdapter(
    host: EmbedderHost,
    progressCallbackStore: (WasmPtr<SqliteDb>) -> SqliteProgressCallback?,
) {
    private val handle = Sqlite3ProgressFunctionHandle(host, progressCallbackStore)
    val function: ChasmHostFunction = { args ->
        val result = handle.execute(args[0].asWasmAddr())
        listOf(Value.Number.I32(result))
    }
}
