/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.driver.chicory

import at.released.wasm.sqlite.binary.SqliteAndroidWasmEmscriptenIcu349
import at.released.wasm.sqlite.binary.base.WasmSqliteConfiguration
import at.released.wasm.sqlite.driver.WasmSQLiteDriver
import at.released.wasm.sqlite.driver.base.defaultTestSqliteDriverConfig
import at.released.wasm.sqlite.driver.test.base.tests.TestSqliteDriverFactory
import at.released.wasm.sqlite.open.helper.chicory.ChicoryRuntime
import at.released.wasm.sqlite.open.helper.chicory.ChicorySqliteEmbedder
import at.released.weh.common.api.Logger
import com.dylibso.chicory.experimental.aot.AotMachine

class ChicorySqliteDriverFactory(
    private val useAot: Boolean = false,
) : TestSqliteDriverFactory<WasmSQLiteDriver<ChicoryRuntime>> {
    override val defaultSqliteBinary: WasmSqliteConfiguration = SqliteAndroidWasmEmscriptenIcu349

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
