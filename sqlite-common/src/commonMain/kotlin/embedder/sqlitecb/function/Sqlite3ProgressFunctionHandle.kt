/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function

import at.released.weh.host.EmbedderHost
import at.released.weh.host.base.WasmPtr
import at.released.weh.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback

public class Sqlite3ProgressFunctionHandle(
    host: EmbedderHost,
    private val progressCallbackStore: (WasmPtr<SqliteDb>) -> SqliteProgressCallback?,
) : HostFunctionHandle(SqliteCallbacksModuleFunction.SQLITE3_LOGGING_CALLBACK, host) {
    public fun execute(
        contextPointer: WasmPtr<SqliteDb>,
    ): Int {
        logger.v { "Calling progress callback for pinter: $contextPointer" }
        val delegate: SqliteProgressCallback = progressCallbackStore(contextPointer)
            ?: error("Callback $contextPointer not registered")

        return delegate.invoke(contextPointer)
    }
}
