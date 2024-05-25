/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.chicory

import ru.pixnews.wasm.sqlite.driver.base.TestSqliteDriverCreator
import ru.pixnews.wasm.sqlite.open.helper.SqliteAndroidWasmEmscriptenIcu346
import ru.pixnews.wasm.sqlite.open.helper.chicory.ChicorySqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.chicory.ChicorySqliteEmbedderConfig

object ChicorySqliteDriverCreator : TestSqliteDriverCreator<ChicorySqliteEmbedderConfig>(
    embedder = ChicorySqliteEmbedder,
    defaultSqliteBinary = SqliteAndroidWasmEmscriptenIcu346,
    defaultEmbedderConfig = { sqlite3Binary ->
        this.sqlite3Binary = sqlite3Binary
    },
)
