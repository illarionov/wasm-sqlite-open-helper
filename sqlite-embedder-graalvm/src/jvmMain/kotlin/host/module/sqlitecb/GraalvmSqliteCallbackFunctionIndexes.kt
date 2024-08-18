/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.sqlitecb

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.SqliteCallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_LOGGING_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_PROGRESS_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.embedder.sqlitecb.SqliteCallbacksModuleFunction.SQLITE3_TRACE_CALLBACK
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.threadfactory.ExternalManagedThreadOrchestrator.Companion.USE_MANAGED_THREAD_PTHREAD_ROUTINE_FUNCTION
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.IndirectFunctionTableIndex

@InternalWasmSqliteHelperApi
internal class GraalvmSqliteCallbackFunctionIndexes(
    functionMap: Map<HostFunction, IndirectFunctionTableIndex>,
) : SqliteCallbackFunctionIndexes {
    override val traceFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_TRACE_CALLBACK)
    override val progressFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_PROGRESS_CALLBACK)
    override val loggingCallbackFunction: IndirectFunctionTableIndex = functionMap.getValue(SQLITE3_LOGGING_CALLBACK)
    val useManagedThreadPthreadRoutineFunction: IndirectFunctionTableIndex =
        functionMap.getValue(USE_MANAGED_THREAD_PTHREAD_ROUTINE_FUNCTION)
}
