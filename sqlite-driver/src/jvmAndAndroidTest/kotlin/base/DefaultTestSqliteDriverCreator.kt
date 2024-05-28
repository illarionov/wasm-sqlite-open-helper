/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.base

import ru.pixnews.wasm.sqlite.driver.dsl.WasmSqliteDriverConfigBlock
import ru.pixnews.wasm.sqlite.open.helper.common.api.Locale
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.dsl.path.DatabasePathResolver
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import java.io.File

fun <E : SqliteEmbedderConfig> WasmSqliteDriverConfigBlock<E>.defaultTestSqliteDriverConfig(
    dstDir: String,
    dbLogger: Logger,
) {
    pathResolver = DatabasePathResolver { name -> File(dstDir, name).path }
    logger = dbLogger
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
}
