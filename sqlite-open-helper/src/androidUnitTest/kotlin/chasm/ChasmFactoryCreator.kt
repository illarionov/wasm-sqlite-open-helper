/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.chasm

import androidx.sqlite.db.SupportSQLiteOpenHelper
import at.released.wasm.sqlite.binary.SqliteAndroidWasmEmscriptenIcu349
import at.released.wasm.sqlite.binary.base.WasmSqliteConfiguration
import at.released.wasm.sqlite.open.helper.WasmSqliteOpenHelperFactory
import at.released.wasm.sqlite.open.helper.base.util.defaultTestHelperConfig
import at.released.wasm.sqlite.open.helper.test.base.TestOpenHelperFactoryCreator
import at.released.weh.common.api.Logger

object ChasmFactoryCreator : TestOpenHelperFactoryCreator {
    override val defaultSqliteBinary: WasmSqliteConfiguration = SqliteAndroidWasmEmscriptenIcu349
    override fun create(
        dstDir: String,
        dbLogger: Logger,
        sqlite3Binary: WasmSqliteConfiguration,
    ): SupportSQLiteOpenHelper.Factory {
        return WasmSqliteOpenHelperFactory(ChasmSqliteEmbedder) {
            defaultTestHelperConfig(dstDir, dbLogger)
            embedder {
                this.sqlite3Binary = sqlite3Binary
            }
        }
    }
}
