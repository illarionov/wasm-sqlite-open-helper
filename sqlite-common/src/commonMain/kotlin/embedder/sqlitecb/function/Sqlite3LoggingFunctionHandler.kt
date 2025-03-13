/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.embedder.sqlitecb.function

import at.released.wasm.sqlite.open.helper.WasmPtr
import at.released.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteLogCallback
import at.released.weh.host.EmbedderHost
import at.released.weh.wasm.core.memory.ReadOnlyMemory
import at.released.weh.wasm.core.memory.readNullTerminatedString

public class Sqlite3LoggingFunctionHandler(
    host: EmbedderHost,
    private val logCallbackStore: () -> SqliteLogCallback?,
) : SqliteHostFunctionHandle(SqliteCallbacksModuleFunction.SQLITE3_LOGGING_CALLBACK, host) {
    public fun execute(
        memory: ReadOnlyMemory,
        errCode: Int,
        messagePointer: WasmPtr<Byte>,
    ) {
        val delegate = logCallbackStore() ?: return
        val message = memory.readNullTerminatedString(messagePointer.addr)
        delegate.invoke(errCode, message)
    }
}
