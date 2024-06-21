/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.debug

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfigBlock.Factory.Type
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig

/**
 * Configures SQLite [Global error log](https://www.sqlite.org/errlog.html)
 *
 */
public class SqliteErrorLogger private constructor(
    public var logger: (errCode: Int, message: String) -> Unit,
) : WasmSqliteDebugFeature {
    @InternalWasmSqliteHelperApi
    override val key: WasmSqliteDebugConfigBlock.Key<*> = Companion
    public var enabled: Boolean = true

    public companion object : WasmSqliteDebugConfigBlock.Key<SqliteErrorLogger> {
        override fun create(commonConfig: WasmSqliteCommonConfig, type: Type): SqliteErrorLogger {
            val logger = commonConfig.logger.withTag("SQLite Error")
            return SqliteErrorLogger(createStatementLogger(logger))
        }

        public fun createStatementLogger(
            logger: Logger,
        ): (Int, String) -> Unit = { errCode, message -> logger.w { "$errCode: $message" } }
    }
}
