/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.base.util

import at.released.wasm.sqlite.open.helper.Locale
import at.released.wasm.sqlite.open.helper.debug.SqliteSlowQueryLogger
import at.released.wasm.sqlite.open.helper.debug.SqliteStatementLogger
import at.released.wasm.sqlite.open.helper.debug.SqliteStatementProfileLogger
import at.released.wasm.sqlite.open.helper.dsl.WasmSqliteOpenHelperFactoryConfigBlock
import at.released.wasm.sqlite.open.helper.dsl.path.DatabasePathResolver
import at.released.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import at.released.weh.common.api.Logger
import java.io.File

fun <E : SqliteEmbedderConfig> WasmSqliteOpenHelperFactoryConfigBlock<E>.defaultTestHelperConfig(
    dstDir: String,
    dbLogger: Logger,
) {
    pathResolver = DatabasePathResolver { name -> File(dstDir, name).path }
    logger = dbLogger
    debug {
        set(SqliteStatementLogger)
        set(SqliteStatementProfileLogger)
        set(SqliteSlowQueryLogger)
    }
    openParams {
        locale = Locale("ru_RU")
        setLookasideConfig(
            slotSize = 1200,
            slotCount = 100,
        )
    }
}
