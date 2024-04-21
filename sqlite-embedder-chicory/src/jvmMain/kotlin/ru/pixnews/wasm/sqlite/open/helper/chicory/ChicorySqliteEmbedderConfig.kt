/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory

import ru.pixnews.wasm.sqlite.open.helper.SqliteAndroidWasmEmscriptenIcu345
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteOpenHelperDsl
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig

/**
 * Configuration if the GraalVM engine
 */
@WasmSqliteOpenHelperDsl
public class ChicorySqliteEmbedderConfig internal constructor(
    rootLogger: Logger,
) : SqliteEmbedderConfig {
    /**
     * Sets used Sqlite WebAssembly binary file
     */
    public var sqlite3Binary: WasmSqliteConfiguration = SqliteAndroidWasmEmscriptenIcu345

    /**
     * Implementation of a host object that provides access from the WebAssembly to external host resources.
     */
    public var host: SqliteEmbedderHost = SqliteEmbedderHost(rootLogger = rootLogger)
}
