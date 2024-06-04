/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.chasm

import androidx.sqlite.SQLiteDriver
import ru.pixnews.wasm.sqlite.binary.SqliteAndroidWasmEmscriptenIcu346
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.driver.WasmSQLiteDriver
import ru.pixnews.wasm.sqlite.driver.base.defaultTestSqliteDriverConfig
import ru.pixnews.wasm.sqlite.driver.test.base.tests.TestSqliteDriverFactory
import ru.pixnews.wasm.sqlite.open.helper.chasm.ChasmSqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger

object ChasmSqliteDriverFactory : TestSqliteDriverFactory {
    override val defaultSqliteBinary: WasmSqliteConfiguration = SqliteAndroidWasmEmscriptenIcu346
    override fun create(dbLogger: Logger, sqlite3Binary: WasmSqliteConfiguration): SQLiteDriver {
        return WasmSQLiteDriver(ChasmSqliteEmbedder) {
            defaultTestSqliteDriverConfig(dbLogger)
            embedder {
                this.sqlite3Binary = sqlite3Binary
            }
        }
    }
}
