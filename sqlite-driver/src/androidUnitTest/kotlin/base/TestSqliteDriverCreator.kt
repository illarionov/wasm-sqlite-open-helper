/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.base

import androidx.sqlite.SQLiteDriver
import ru.pixnews.wasm.sqlite.driver.WasmSQLiteDriver
import ru.pixnews.wasm.sqlite.driver.dsl.WasmSqliteDriverConfigBlock
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.common.api.Locale
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.dsl.path.DatabasePathResolver
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import java.io.File

abstract class TestSqliteDriverCreator<C : SqliteEmbedderConfig>(
    val embedder: SqliteEmbedder<C>,
    val defaultEmbedderConfig: C.(sqlite3Binary: WasmSqliteConfiguration) -> Unit,
    val defaultSqliteBinary: WasmSqliteConfiguration,
) {
    open fun create(
        dstDir: File,
        dbLogger: Logger,
        sqlite3Binary: WasmSqliteConfiguration = defaultSqliteBinary,
        extraConfig: WasmSqliteDriverConfigBlock<C>.() -> Unit = {},
    ): SQLiteDriver {
        return WasmSQLiteDriver(embedder) {
            pathResolver = DatabasePathResolver { name -> File(dstDir, name).path }
            logger = dbLogger
            embedder {
                defaultEmbedderConfig(sqlite3Binary)
            }
            debug {
                sqlLog = true
                sqlTime = true
                sqlStatements = true
                logSlowQueries = true
            }
            openParams {
                locale = Locale("ru_RU")
                setLookasideConfig(
                    slotSize = 1200,
                    slotCount = 100,
                )
            }
            extraConfig()
        }
    }
}
