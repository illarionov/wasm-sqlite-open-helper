/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore.SqliteComparatorId
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteComparatorCallback

public class SqliteComparatorFunctionHandle(
    host: SqliteEmbedderHost,
    private val comparatorStore: (SqliteComparatorId) -> SqliteComparatorCallback?,
) : HostFunctionHandle(SqliteCallbacksModuleFunction.SQLITE3_COMPARATOR_CALL_CALLBACK, host) {
    public fun execute(
        memory: Memory,
        comparatorId: Int,
        str1Size: Int,
        str1: WasmPtr<Byte>,
        str2Size: Int,
        str2: WasmPtr<Byte>,
    ): Int {
        logger.v { "Calling comparator: $comparatorId" }
        val delegate: SqliteComparatorCallback = comparatorStore(SqliteComparatorId(comparatorId))
            ?: error("Comparator $comparatorId not registered")

        val str1Bytes = memory.readBytes(str1, str1Size)
        val str2Bytes = memory.readBytes(str2, str2Size)

        return delegate.invoke(String(str1Bytes), String(str2Bytes))
    }
}
