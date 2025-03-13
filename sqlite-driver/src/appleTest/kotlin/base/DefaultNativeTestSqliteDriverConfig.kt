/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.driver.base

import at.released.wasm.sqlite.driver.dsl.WasmSqliteDriverConfigBlock
import at.released.wasm.sqlite.open.helper.Locale
import at.released.wasm.sqlite.open.helper.debug.SqliteStatementLogger
import at.released.wasm.sqlite.open.helper.debug.SqliteStatementProfileLogger
import at.released.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import at.released.weh.common.api.Logger

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
