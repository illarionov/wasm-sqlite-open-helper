/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.chasm

import androidx.sqlite.SQLiteDriver
import ru.pixnews.wasm.sqlite.driver.WasmSQLiteDriver
import ru.pixnews.wasm.sqlite.driver.base.defaultTestSqliteDriverConfig
import ru.pixnews.wasm.sqlite.driver.test.base.TestSqliteDriverCreator
import ru.pixnews.wasm.sqlite.open.helper.SqliteAndroidWasmEmscriptenIcu346
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.chasm.ChasmSqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import java.io.File

object ChasmSqliteDriverCreator : TestSqliteDriverCreator {
    override val defaultSqliteBinary: WasmSqliteConfiguration = SqliteAndroidWasmEmscriptenIcu346
    override fun create(dstDir: File, dbLogger: Logger, sqlite3Binary: WasmSqliteConfiguration): SQLiteDriver {
        return WasmSQLiteDriver(ChasmSqliteEmbedder) {
            defaultTestSqliteDriverConfig(dstDir, dbLogger)
            embedder {
                this.sqlite3Binary = sqlite3Binary
            }
        }
    }
}
