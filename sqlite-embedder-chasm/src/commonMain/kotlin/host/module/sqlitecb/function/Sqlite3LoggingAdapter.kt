/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.sqlitecb.function

import at.released.weh.host.EmbedderHost
import at.released.weh.host.base.memory.ReadOnlyMemory
import io.github.charlietap.chasm.embedding.shapes.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asInt
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function.Sqlite3LoggingFunctionHandler
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteLogCallback

internal class Sqlite3LoggingAdapter(
    host: EmbedderHost,
    private val memory: ReadOnlyMemory,
    logCallbackStore: () -> SqliteLogCallback?,
) {
    private val handle = Sqlite3LoggingFunctionHandler(host, logCallbackStore)
    val function: HostFunction = { args ->
        handle.execute(
            memory,
            // unused context pointer args.getArgAsWasmPtr(0),
            args[1].asInt(),
            args[2].asWasmAddr(),
        )
        emptyList()
    }
}
