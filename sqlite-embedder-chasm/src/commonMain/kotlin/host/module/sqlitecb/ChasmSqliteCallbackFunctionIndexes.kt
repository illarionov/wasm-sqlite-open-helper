/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.sqlitecb

import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.IndirectFunctionTableIndex
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.SqliteCallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction

internal class ChasmSqliteCallbackFunctionIndexes(
    functionMap: Map<SqliteCallbacksModuleFunction, IndirectFunctionTableIndex>,
) : SqliteCallbackFunctionIndexes {
    override val traceFunction: IndirectFunctionTableIndex =
        functionMap.getValue(SqliteCallbacksModuleFunction.SQLITE3_TRACE_CALLBACK)
    override val progressFunction: IndirectFunctionTableIndex =
        functionMap.getValue(SqliteCallbacksModuleFunction.SQLITE3_PROGRESS_CALLBACK)
    override val loggingCallbackFunction: IndirectFunctionTableIndex =
        functionMap.getValue(SqliteCallbacksModuleFunction.SQLITE3_LOGGING_CALLBACK)
}
