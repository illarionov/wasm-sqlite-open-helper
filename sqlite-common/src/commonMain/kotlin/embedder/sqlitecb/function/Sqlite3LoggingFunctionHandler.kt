/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.function

import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.ReadOnlyMemory
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.readNullTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteLogCallback

public class Sqlite3LoggingFunctionHandler(
    host: EmbedderHost,
    private val logCallbackStore: () -> SqliteLogCallback?,
) : HostFunctionHandle(SqliteCallbacksModuleFunction.SQLITE3_LOGGING_CALLBACK, host) {
    public fun execute(
        memory: ReadOnlyMemory,
        errCode: Int,
        messagePointer: WasmPtr<Byte>,
    ) {
        val delegate = logCallbackStore() ?: return
        val message = memory.readNullTerminatedString(messagePointer)
        delegate.invoke(errCode, message)
    }
}
