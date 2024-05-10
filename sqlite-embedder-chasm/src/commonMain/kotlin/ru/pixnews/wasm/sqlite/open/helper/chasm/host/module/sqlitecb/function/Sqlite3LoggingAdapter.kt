/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.sqlitecb.function

import io.github.charlietap.chasm.executor.runtime.value.ExecutionValue
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asInt
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.EmscriptenHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function.Sqlite3LoggingFunctionHandler
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteLogCallback

internal class Sqlite3LoggingAdapter(
    host: EmbedderHost,
    private val memory: Memory,
    logCallbackStore: () -> SqliteLogCallback?,
) : EmscriptenHostFunctionHandle {
    private val handle = Sqlite3LoggingFunctionHandler(host, logCallbackStore)

    override fun invoke(args: List<ExecutionValue>): List<ExecutionValue> {
        handle.execute(
            memory,
            // unused context pointer args.getArgAsWasmPtr(0),
            args[1].asInt(),
            args[2].asWasmAddr(),
        )
        return emptyList()
    }
}
