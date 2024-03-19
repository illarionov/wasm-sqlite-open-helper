/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper

import androidx.sqlite.db.SupportSQLiteOpenHelper
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.dsl.WasmSqliteOpenHelperFactoryConfigBlock
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import ru.pixnews.wasm.sqlite.open.helper.internal.CloseGuard
import ru.pixnews.wasm.sqlite.open.helper.internal.CloseGuard.Reporter
import ru.pixnews.wasm.sqlite.open.helper.path.JvmDatabasePathResolver

/**
 * Creates a [SupportSQLiteOpenHelper.Factory] with the specified [block] configuration.
 *
 * @param sqliteCapi Implementation of the required functions from Sqlite C API. For example, GraalvmSqliteCapi
 */
@Suppress("FunctionName")
public fun <E : SqliteEmbedderConfig> WasmSqliteOpenHelperFactory(
    embedder: SqliteEmbedder<E>,
    block: WasmSqliteOpenHelperFactoryConfigBlock<E>.() -> Unit,
): SupportSQLiteOpenHelper.Factory {
    val config = WasmSqliteOpenHelperFactoryConfigBlock<E>(JvmDatabasePathResolver()).apply(block)
    val commonConfig = object : WasmSqliteCommonConfig {
        override val logger: Logger = config.logger
    }

    setupCloseGuard(config.logger)

    return WasmSqliteOpenHelperFactory(
        pathResolver = config.pathResolver,
        defaultLocale = config.locale,
        sqliteCapi = embedder.createCapi(commonConfig, config.embedderConfig),
        debugConfig = config.debugConfigBlock.build(),
        rootLogger = commonConfig.logger,
        configurationOptions = config.configurationOptions,
    )
}

private fun setupCloseGuard(rootLogger: Logger) {
    val logger = rootLogger.withTag("SQLite")
    CloseGuard.reporter = Reporter { message, allocationSite ->
        logger.w(allocationSite, message::toString)
    }
}
