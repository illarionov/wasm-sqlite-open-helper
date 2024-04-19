/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.interop.capi

import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteConfigParameter
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteLogCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode

/**
 * Wrappers for SQLite3 C Api configuration functions: `sqlite3_config`, `sqlite3_initialize`, …
 */
internal class Sqlite3ConfigFunctions(
    private val sqliteBindings: SqliteBindings,
    private val callbackStore: SqliteCallbackStore,
    private val callbackFunctionIndexes: Sqlite3CallbackFunctionIndexes,
) {
    fun sqlite3initialize(): SqliteResultCode {
        return sqliteBindings.sqlite3_initialize.executeForSqliteResultCode()
    }

    fun sqlite3Config(op: SqliteConfigParameter, arg1: Int): SqliteResultCode {
        return sqliteBindings.sqlite3__wasm_config_i.executeForSqliteResultCode(op.id, arg1)
    }

    fun sqlite3SetLogger(logger: SqliteLogCallback?): SqliteResultCode {
        val oldLogger = callbackStore.sqlite3LogCallback
        return try {
            callbackStore.sqlite3LogCallback = logger
            sqliteBindings.sqlite3__wasm_config_ii.executeForSqliteResultCode(
                SqliteConfigParameter.SQLITE_CONFIG_LOG.id,
                if (logger != null) callbackFunctionIndexes.loggingCallbackFunction.funcId else 0,
                0,
            )
        } catch (@Suppress("TooGenericExceptionCaught") ex: Throwable) {
            callbackStore.sqlite3LogCallback = oldLogger
            throw ex
        }
    }

    fun sqlite3SoftHeapLimit(limit: Long): SqliteResultCode {
        return sqliteBindings.sqlite3_soft_heap_limit64.executeForSqliteResultCode(limit)
    }
}
