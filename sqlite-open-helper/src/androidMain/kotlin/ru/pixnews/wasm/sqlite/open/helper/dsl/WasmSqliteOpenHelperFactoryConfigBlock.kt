/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.dsl

import androidx.sqlite.db.SupportSQLiteOpenHelper
import ru.pixnews.wasm.sqlite.open.helper.ConfigurationOptions
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteOpenHelperFactory
import ru.pixnews.wasm.sqlite.open.helper.path.DatabasePathResolver
import ru.pixnews.wasm.sqlite.open.helper.path.JvmDatabasePathResolver
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteCapi

@WasmSqliteOpenHelperDsl
public class WasmSqliteOpenHelperFactoryConfigBlock(
    private val sqliteCapi: SqliteCapi,
) {
    private var debugConfigBlock: DebugConfigBlock = DebugConfigBlock()
    public var pathResolver: DatabasePathResolver = JvmDatabasePathResolver()
    private var configurationOptions: List<ConfigurationOptions> = emptyList()

    public fun debug(block: DebugConfigBlock.() -> Unit) {
        debugConfigBlock = DebugConfigBlock().apply(block)
    }

    public fun configurationOptions(block: MutableList<ConfigurationOptions>.() -> Unit) {
        configurationOptions = buildList(block)
    }

    internal fun build(): SupportSQLiteOpenHelper.Factory {
        return WasmSqliteOpenHelperFactory(
            pathResolver = pathResolver,
            sqliteCapi = sqliteCapi,
            debugConfig = debugConfigBlock.build(),
            configurationOptions = configurationOptions,
        )
    }
}
