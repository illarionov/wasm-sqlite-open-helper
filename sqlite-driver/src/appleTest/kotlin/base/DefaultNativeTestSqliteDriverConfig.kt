/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.base

import ru.pixnews.wasm.sqlite.driver.dsl.WasmSqliteDriverConfigBlock
import ru.pixnews.wasm.sqlite.open.helper.common.api.Locale
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.debug.SqliteStatementLogger
import ru.pixnews.wasm.sqlite.open.helper.debug.SqliteStatementProfileLogger
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig

internal fun <E : SqliteEmbedderConfig> WasmSqliteDriverConfigBlock<E>.defaultNativeTestSqliteDriverConfig(
    dbLogger: Logger,
) {
    logger = dbLogger
    debug {
        set(SqliteStatementLogger)
        set(SqliteStatementProfileLogger)
    }
    openParams {
        locale = Locale("ru_RU")
    }
}
