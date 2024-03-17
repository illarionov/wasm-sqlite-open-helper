/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback

import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func.SQLITE3_COMPARATOR_CALL_FUNCTION_NAME
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func.SQLITE3_DESTROY_COMPARATOR_FUNCTION_NAME
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func.SQLITE3_EXEC_CB_FUNCTION_NAME
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func.SQLITE3_PROGRESS_CB_FUNCTION_NAME
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.func.SQLITE3_TRACE_CB_FUNCTION_NAME
import ru.pixnews.wasm.sqlite.open.helper.host.functiontable.IndirectFunctionTableIndex

internal class Sqlite3CallbackFunctionIndexes(
    functionMap: Map<String, IndirectFunctionTableIndex>,
) {
    val execCallbackFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_EXEC_CB_FUNCTION_NAME)
    val traceFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_TRACE_CB_FUNCTION_NAME)
    val progressFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_PROGRESS_CB_FUNCTION_NAME)
    val comparatorFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_COMPARATOR_CALL_FUNCTION_NAME)
    val destroyComparatorFunction: IndirectFunctionTableIndex = functionMap.getValue(
        SQLITE3_DESTROY_COMPARATOR_FUNCTION_NAME,
    )
}
