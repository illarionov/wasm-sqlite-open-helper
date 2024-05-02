/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm

import org.graalvm.polyglot.Engine
import ru.pixnews.wasm.sqlite.open.helper.SqliteAndroidWasmEmscriptenIcuMtPthread345
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteOpenHelperDsl
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import ru.pixnews.wasm.sqlite.open.helper.host.JvmSqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost

/**
 * Configuration if the GraalVM engine
 */
@WasmSqliteOpenHelperDsl
public class GraalvmSqliteEmbedderConfig internal constructor(
    rootLogger: Logger,
) : SqliteEmbedderConfig {
    /**
     * Instance of the GraalVM WebAssembly engine. Single instance of the Engine can be reused to speed
     * up initialization.
     *
     * Should be `Engine.create("wasm")`.
     *
     * See: [GraalVM Managing the code cache](https://www.graalvm.org/latest/reference-manual/embed-languages/#managing-the-code-cache)
     */
    public var graalvmEngine: Engine = Engine.create("wasm")

    /**
     * Sets used Sqlite WebAssembly binary file
     */
    public var sqlite3Binary: WasmSqliteConfiguration = SqliteAndroidWasmEmscriptenIcuMtPthread345

    /**
     * Implementation of a host object that provides access from the WebAssembly to external host resources.
     */
    public var host: SqliteEmbedderHost = JvmSqliteEmbedderHost(rootLogger = rootLogger)
}
