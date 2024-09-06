/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.chicory

import at.released.weh.common.api.Logger
import com.dylibso.chicory.aot.AotMachine
import ru.pixnews.wasm.sqlite.binary.SqliteAndroidWasmEmscriptenIcu346
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.driver.WasmSQLiteDriver
import ru.pixnews.wasm.sqlite.driver.base.defaultTestSqliteDriverConfig
import ru.pixnews.wasm.sqlite.driver.test.base.tests.TestSqliteDriverFactory
import ru.pixnews.wasm.sqlite.open.helper.chicory.ChicoryRuntime
import ru.pixnews.wasm.sqlite.open.helper.chicory.ChicorySqliteEmbedder

class ChicorySqliteDriverFactory(
    private val useAot: Boolean = false,
) : TestSqliteDriverFactory<WasmSQLiteDriver<ChicoryRuntime>> {
    override val defaultSqliteBinary: WasmSqliteConfiguration = SqliteAndroidWasmEmscriptenIcu346

    override fun create(
        dbLogger: Logger,
        sqlite3Binary: WasmSqliteConfiguration,
    ): WasmSQLiteDriver<ChicoryRuntime> {
        return WasmSQLiteDriver(ChicorySqliteEmbedder) {
            defaultTestSqliteDriverConfig(dbLogger)
            embedder {
                this.sqlite3Binary = sqlite3Binary
                if (useAot) {
                    this.machineFactory = ::AotMachine
                }
            }
        }
    }
}
