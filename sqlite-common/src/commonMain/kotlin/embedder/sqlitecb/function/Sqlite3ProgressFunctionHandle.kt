/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.embedder.sqlitecb.function

import at.released.wasm.sqlite.open.helper.WasmPtr
import at.released.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback
import at.released.weh.host.EmbedderHost

public class Sqlite3ProgressFunctionHandle(
    host: EmbedderHost,
    private val progressCallbackStore: (WasmPtr<SqliteDb>) -> SqliteProgressCallback?,
) : SqliteHostFunctionHandle(SqliteCallbacksModuleFunction.SQLITE3_LOGGING_CALLBACK, host) {
    public fun execute(
        contextPointer: WasmPtr<SqliteDb>,
    ): Int {
        logger.v { "Calling progress callback for pointer: $contextPointer" }
        val delegate: SqliteProgressCallback = progressCallbackStore(contextPointer)
            ?: error("Callback $contextPointer not registered")

        return delegate.invoke(contextPointer)
    }
}
