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
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore.SqliteComparatorId
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function.SqliteComparatorFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteComparatorCallback

public class Sqlite3ComparatorAdapter(
    host: EmbedderHost,
    private val memory: Memory,
    comparatorStore: (SqliteComparatorId) -> SqliteComparatorCallback?,
    ) : WasmFunctionHandle {
    private val handle = SqliteComparatorFunctionHandle(host, comparatorStore)

    override fun apply(instance: Instance, vararg args: Value): Array<Value> {
        val result = handle.execute(
            memory,
            args[0].asInt(),
            args[1].asInt(),
            args[2].asWasmAddr(),
            args[3].asInt(),
            args[4].asWasmAddr(),
        )
        return arrayOf(Value.i32(result.toLong()))
    }
}
