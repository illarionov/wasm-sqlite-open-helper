/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm

import org.graalvm.polyglot.Engine
import ru.pixnews.wasm.sqlite.open.helper.Sqlite3Wasm
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteOpenHelperDsl
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig

@WasmSqliteOpenHelperDsl
public class GraalvmSqliteEmbedderConfig internal constructor(
    rootLogger: Logger,
) : SqliteEmbedderConfig {
    public var graalvmEngine: Engine = Engine.create("wasm")
    public var sqlite3Binary: WasmSqliteConfiguration = Sqlite3Wasm.Emscripten.sqlite3_345_android_icu_mt_pthread
    public var host: SqliteEmbedderHost = SqliteEmbedderHost(rootLogger = rootLogger)
}
