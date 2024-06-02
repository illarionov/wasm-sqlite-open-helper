/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm

import ru.pixnews.wasm.sqlite.binary.SqliteAndroidWasmEmscriptenIcu346
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteOpenHelperDsl
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.JvmEmbedderHost

/**
 * Configuration of the Chasm engine
 */
@WasmSqliteOpenHelperDsl
public class ChasmSqliteEmbedderConfig internal constructor(
    rootLogger: Logger,
) : SqliteEmbedderConfig {
    /**
     * Used Sqlite WebAssembly binary file
     */
    public var sqlite3Binary: WasmSqliteConfiguration = SqliteAndroidWasmEmscriptenIcu346

    /**
     * Implementation of a host object that provides access from the WebAssembly to external host resources.
     */
    public var host: EmbedderHost = JvmEmbedderHost(rootLogger = rootLogger)
}
