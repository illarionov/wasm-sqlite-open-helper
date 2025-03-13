/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.driver.graalvm

import at.released.wasm.sqlite.binary.SqliteAndroidWasmEmscriptenIcuMtPthread349
import at.released.wasm.sqlite.binary.base.WasmSqliteConfiguration
import at.released.wasm.sqlite.driver.WasmSQLiteDriver
import at.released.wasm.sqlite.driver.base.defaultTestSqliteDriverConfig
import at.released.wasm.sqlite.driver.test.base.tests.TestSqliteDriverFactory
import at.released.wasm.sqlite.open.helper.graalvm.GraalvmRuntime
import at.released.wasm.sqlite.open.helper.graalvm.GraalvmSqliteEmbedder
import at.released.weh.common.api.Logger
import org.graalvm.polyglot.Engine

class GraalvmSqliteDriverFactory(
    private val initialGraalvmEngine: Engine? = WASM_GRAALVM_ENGINE,
    override val defaultSqliteBinary: WasmSqliteConfiguration = SqliteAndroidWasmEmscriptenIcuMtPthread349,
) : TestSqliteDriverFactory<WasmSQLiteDriver<GraalvmRuntime>> {
    override fun create(
        dbLogger: Logger,
        sqlite3Binary: WasmSqliteConfiguration,
    ): WasmSQLiteDriver<GraalvmRuntime> {
        return WasmSQLiteDriver(GraalvmSqliteEmbedder) {
            defaultTestSqliteDriverConfig(dbLogger)
            embedder {
                this.sqlite3Binary = sqlite3Binary
                if (initialGraalvmEngine != null) {
                    this.graalvmEngine = initialGraalvmEngine
                }
            }
        }
    }

    companion object {
        internal val WASM_GRAALVM_ENGINE: Engine = Engine.create("wasm")
    }
}
