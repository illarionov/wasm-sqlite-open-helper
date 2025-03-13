/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.chasm

import at.released.cassettes.playhead.AppleNsBundleAssetManager
import at.released.wasm.sqlite.binary.SqliteAndroidWasmEmscriptenIcu349
import at.released.wasm.sqlite.binary.base.WasmSqliteConfiguration
import at.released.weh.common.api.Logger
import ru.pixnews.wasm.sqlite.driver.WasmSQLiteDriver
import ru.pixnews.wasm.sqlite.driver.base.defaultNativeTestSqliteDriverConfig
import ru.pixnews.wasm.sqlite.driver.test.base.tests.TestSqliteDriverFactory
import ru.pixnews.wasm.sqlite.open.helper.chasm.ChasmRuntime
import ru.pixnews.wasm.sqlite.open.helper.chasm.ChasmSqliteEmbedder

object ChasmAppleSqliteDriverFactory : TestSqliteDriverFactory<WasmSQLiteDriver<ChasmRuntime>> {
    override val defaultSqliteBinary: WasmSqliteConfiguration = SqliteAndroidWasmEmscriptenIcu349
    override fun create(
        dbLogger: Logger,
        sqlite3Binary: WasmSqliteConfiguration,
    ): WasmSQLiteDriver<ChasmRuntime> {
        return WasmSQLiteDriver(ChasmSqliteEmbedder) {
            defaultNativeTestSqliteDriverConfig(dbLogger)
            embedder {
                this.sqlite3Binary = sqlite3Binary
                this.wasmSourceReader = AppleNsBundleAssetManager(wshohResourcesRoot = "cassettes")
            }
        }
    }
}
