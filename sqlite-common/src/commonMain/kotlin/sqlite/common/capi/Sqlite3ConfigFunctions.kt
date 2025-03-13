/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.sqlite.common.capi

import at.released.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import at.released.wasm.sqlite.open.helper.embedder.exports.SqliteExports
import at.released.wasm.sqlite.open.helper.embedder.functiontable.SqliteCallbackFunctionIndexes
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteConfigParameter
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteLogCallback
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode

/**
 * Wrappers for SQLite3 C Api configuration functions: `sqlite3_config`, `sqlite3_initialize`, …
 */
public class Sqlite3ConfigFunctions internal constructor(
    private val sqliteExports: SqliteExports,
    private val callbackStore: SqliteCallbackStore,
    private val callbackFunctionIndexes: SqliteCallbackFunctionIndexes,
) {
    public fun sqlite3initialize(): SqliteResultCode {
        return sqliteExports.sqlite3_initialize.executeForSqliteResultCode()
    }

    public fun sqlite3Config(op: SqliteConfigParameter, arg1: Int): SqliteResultCode {
        return sqliteExports.sqlite3__wasm_config_i.executeForSqliteResultCode(op.id, arg1)
    }

    public fun sqlite3SetLogger(logger: SqliteLogCallback?): SqliteResultCode {
        val oldLogger = callbackStore.sqlite3LogCallback
        return try {
            callbackStore.sqlite3LogCallback = logger
            sqliteExports.sqlite3__wasm_config_ii.executeForSqliteResultCode(
                SqliteConfigParameter.SQLITE_CONFIG_LOG.id,
                if (logger != null) callbackFunctionIndexes.loggingCallbackFunction.funcId else 0,
                0,
            )
        } catch (@Suppress("TooGenericExceptionCaught") ex: Throwable) {
            callbackStore.sqlite3LogCallback = oldLogger
            throw ex
        }
    }

    public fun sqlite3SoftHeapLimit(limit: Long): SqliteResultCode {
        val oldLimit = sqliteExports.sqlite3_soft_heap_limit64.executeForLong(limit)
        return if (oldLimit < 0) {
            SqliteResultCode.SQLITE_ERROR
        } else {
            SqliteResultCode.SQLITE_OK
        }
    }
}
