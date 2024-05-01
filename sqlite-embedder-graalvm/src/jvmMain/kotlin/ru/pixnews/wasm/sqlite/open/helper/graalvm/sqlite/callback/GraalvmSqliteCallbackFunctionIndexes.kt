/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.IndirectFunctionTableIndex
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_COMPARATOR_CALL_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_DESTROY_COMPARATOR_FUNCTION
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_EXEC_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_LOGGING_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_PROGRESS_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_TRACE_CALLBACK

@InternalWasmSqliteHelperApi
internal class GraalvmSqliteCallbackFunctionIndexes(
    functionMap: Map<SqliteCallbacksModuleFunction, IndirectFunctionTableIndex>,
) : Sqlite3CallbackFunctionIndexes {
    override val execCallbackFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_EXEC_CALLBACK)
    override val traceFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_TRACE_CALLBACK)
    override val progressFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_PROGRESS_CALLBACK)
    override val comparatorFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_COMPARATOR_CALL_CALLBACK)
    override val destroyComparatorFunction: IndirectFunctionTableIndex = functionMap.getValue(
        SQLITE3_DESTROY_COMPARATOR_FUNCTION,
    )
    override val loggingCallbackFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_LOGGING_CALLBACK)
}
