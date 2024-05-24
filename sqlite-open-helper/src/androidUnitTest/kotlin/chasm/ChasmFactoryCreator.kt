/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm

import ru.pixnews.wasm.sqlite.open.helper.SqliteAndroidWasmEmscriptenIcu346
import ru.pixnews.wasm.sqlite.open.helper.base.TestOpenHelperFactoryCreator

object ChasmFactoryCreator : TestOpenHelperFactoryCreator<ChasmSqliteEmbedderConfig>(
    embedder = ChasmSqliteEmbedder,
    defaultSqliteBinary = SqliteAndroidWasmEmscriptenIcu346,
    defaultEmbedderConfig = { sqlite3Binary ->
        this.sqlite3Binary = sqlite3Binary
    },
)
