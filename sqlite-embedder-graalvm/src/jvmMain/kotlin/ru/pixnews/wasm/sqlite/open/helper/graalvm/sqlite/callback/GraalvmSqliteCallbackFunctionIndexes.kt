/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.IndirectFunctionTableIndex
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func.SQLITE3_COMPARATOR_CALL_FUNCTION_NAME
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func.SQLITE3_DESTROY_COMPARATOR_FUNCTION_NAME
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func.SQLITE3_EXEC_CB_FUNCTION_NAME
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func.SQLITE3_LOGGING_CB_FUNCTION_NAME
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func.SQLITE3_PROGRESS_CB_FUNCTION_NAME
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func.SQLITE3_TRACE_CB_FUNCTION_NAME

@InternalWasmSqliteHelperApi
internal class GraalvmSqliteCallbackFunctionIndexes(
    functionMap: Map<String, IndirectFunctionTableIndex>,
) : Sqlite3CallbackFunctionIndexes {
    override val execCallbackFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_EXEC_CB_FUNCTION_NAME)
    override val traceFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_TRACE_CB_FUNCTION_NAME)
    override val progressFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_PROGRESS_CB_FUNCTION_NAME)
    override val comparatorFunction: IndirectFunctionTableIndex = functionMap.getValue(
        SQLITE3_COMPARATOR_CALL_FUNCTION_NAME,
    )
    override val destroyComparatorFunction: IndirectFunctionTableIndex = functionMap.getValue(
        SQLITE3_DESTROY_COMPARATOR_FUNCTION_NAME,
    )
    override val loggingCallbackFunction: IndirectFunctionTableIndex = functionMap.getValue(
        SQLITE3_LOGGING_CB_FUNCTION_NAME,
    )
}
