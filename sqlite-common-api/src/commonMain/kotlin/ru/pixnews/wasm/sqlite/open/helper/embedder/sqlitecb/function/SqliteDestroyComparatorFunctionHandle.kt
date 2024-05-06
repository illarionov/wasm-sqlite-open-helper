/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function

import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore.SqliteComparatorId
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteComparatorCallback

public class SqliteDestroyComparatorFunctionHandle(
    host: SqliteEmbedderHost,
    private val comparatorStore: SqliteCallbackStore.SqliteCallbackIdMap<SqliteComparatorId, SqliteComparatorCallback>,
) : HostFunctionHandle(SqliteCallbacksModuleFunction.SQLITE3_DESTROY_COMPARATOR_FUNCTION, host) {
    public fun execute(comparatorId: Int) {
        logger.v { "Removing comparator $comparatorId" }
        comparatorStore.remove(SqliteComparatorId(comparatorId))
    }
}
