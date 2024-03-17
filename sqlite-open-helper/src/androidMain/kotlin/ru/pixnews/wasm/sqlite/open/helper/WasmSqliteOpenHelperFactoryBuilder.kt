/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper

import androidx.sqlite.db.SupportSQLiteOpenHelper
import ru.pixnews.wasm.sqlite.open.helper.dsl.WasmSqliteOpenHelperFactoryConfigBlock
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteCapi

/**
 * Creates a [SupportSQLiteOpenHelper.Factory] with the specified [block] configuration.
 *
 * @param sqliteCapi Implementation of the required functions from Sqlite C API. For example, GraalvmSqliteCapi
 */
@Suppress("FunctionName")
public fun WasmSqliteOpenHelperFactory(
    sqliteCapi: SqliteCapi,
    block: WasmSqliteOpenHelperFactoryConfigBlock.() -> Unit,
): SupportSQLiteOpenHelper.Factory {
    return WasmSqliteOpenHelperFactoryConfigBlock(sqliteCapi).apply(block).build()
}
