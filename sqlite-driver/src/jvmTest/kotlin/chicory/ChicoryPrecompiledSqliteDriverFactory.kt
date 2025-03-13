/*
 * Copyright 2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.chicory

import at.released.wasm.sqlite.binary.aot.SqliteAndroidWasmEmscriptenIcuAot349
import at.released.wasm.sqlite.binary.aot.SqliteAndroidWasmEmscriptenIcuAot349Module
import at.released.wasm.sqlite.binary.base.WasmSqliteConfiguration
import at.released.weh.common.api.Logger
import ru.pixnews.wasm.sqlite.driver.WasmSQLiteDriver
import ru.pixnews.wasm.sqlite.driver.base.defaultTestSqliteDriverConfig
import ru.pixnews.wasm.sqlite.driver.test.base.tests.TestSqliteDriverFactory
import ru.pixnews.wasm.sqlite.open.helper.chicory.ChicoryRuntime
import ru.pixnews.wasm.sqlite.open.helper.chicory.ChicorySqliteEmbedder

/**
 * WasmSQLiteDriver factory to run tests with Chicory Runtime and SQLite precompiled to .class files
 */
object ChicoryPrecompiledSqliteDriverFactory : TestSqliteDriverFactory<WasmSQLiteDriver<ChicoryRuntime>> {
    override val defaultSqliteBinary: WasmSqliteConfiguration = SqliteAndroidWasmEmscriptenIcuAot349

    override fun create(
        dbLogger: Logger,
        sqlite3Binary: WasmSqliteConfiguration,
    ): WasmSQLiteDriver<ChicoryRuntime> {
        return WasmSQLiteDriver(ChicorySqliteEmbedder) {
            defaultTestSqliteDriverConfig(dbLogger)
            embedder {
                this.sqlite3Binary = sqlite3Binary
                this.machineFactory = SqliteAndroidWasmEmscriptenIcuAot349Module::create
            }
        }
    }
}
