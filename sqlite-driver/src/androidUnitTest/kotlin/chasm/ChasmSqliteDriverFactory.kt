/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.chasm

import at.released.weh.common.api.Logger
import ru.pixnews.wasm.sqlite.binary.SqliteAndroidWasmEmscriptenIcu348
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.driver.WasmSQLiteDriver
import ru.pixnews.wasm.sqlite.driver.base.defaultTestSqliteDriverConfig
import ru.pixnews.wasm.sqlite.driver.test.base.tests.TestSqliteDriverFactory
import ru.pixnews.wasm.sqlite.open.helper.chasm.ChasmRuntime
import ru.pixnews.wasm.sqlite.open.helper.chasm.ChasmSqliteEmbedder

object ChasmSqliteDriverFactory : TestSqliteDriverFactory<WasmSQLiteDriver<ChasmRuntime>> {
    override val defaultSqliteBinary: WasmSqliteConfiguration = SqliteAndroidWasmEmscriptenIcu348
    override fun create(
        dbLogger: Logger,
        sqlite3Binary: WasmSqliteConfiguration,
    ): WasmSQLiteDriver<ChasmRuntime> {
        return WasmSQLiteDriver(ChasmSqliteEmbedder) {
            defaultTestSqliteDriverConfig(dbLogger)
            embedder {
                this.sqlite3Binary = sqlite3Binary
            }
        }
    }
}
