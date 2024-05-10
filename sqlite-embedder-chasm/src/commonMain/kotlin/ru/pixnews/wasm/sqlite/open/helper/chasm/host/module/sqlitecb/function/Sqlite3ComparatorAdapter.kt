/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.sqlitecb.function

import io.github.charlietap.chasm.executor.runtime.value.ExecutionValue
import io.github.charlietap.chasm.executor.runtime.value.NumberValue.I32
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asInt
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.EmscriptenHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore.SqliteComparatorId
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function.SqliteComparatorFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteComparatorCallback

public class Sqlite3ComparatorAdapter(
    host: EmbedderHost,
    private val memory: Memory,
    comparatorStore: (SqliteComparatorId) -> SqliteComparatorCallback?,
) : EmscriptenHostFunctionHandle {
    private val handle = SqliteComparatorFunctionHandle(host, comparatorStore)

    override fun invoke(args: List<ExecutionValue>): List<ExecutionValue> {
        val result = handle.execute(
            memory,
            args[0].asInt(),
            args[1].asInt(),
            args[2].asWasmAddr(),
            args[3].asInt(),
            args[4].asWasmAddr(),
        )
        return listOf(I32(result))
    }
}
