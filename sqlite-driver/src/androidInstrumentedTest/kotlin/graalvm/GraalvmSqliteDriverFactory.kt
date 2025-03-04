/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.graalvm

import androidx.test.platform.app.InstrumentationRegistry
import at.released.weh.common.api.Logger
import org.graalvm.polyglot.Engine
import ru.pixnews.wasm.sqlite.binary.SqliteAndroidWasmEmscriptenIcuMtPthread349
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.driver.WasmSQLiteDriver
import ru.pixnews.wasm.sqlite.driver.base.defaultTestSqliteDriverConfig
import ru.pixnews.wasm.sqlite.driver.test.base.tests.TestSqliteDriverFactory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.GraalvmRuntime
import ru.pixnews.wasm.sqlite.open.helper.graalvm.GraalvmSqliteEmbedder

class GraalvmSqliteDriverFactory(
    private val initialGraalvmEngine: Engine? = WASM_GRAALVM_ENGINE,
    override val defaultSqliteBinary: WasmSqliteConfiguration = SqliteAndroidWasmEmscriptenIcuMtPthread349,
) : TestSqliteDriverFactory<WasmSQLiteDriver<GraalvmRuntime>> {
    override fun create(
        dbLogger: Logger,
        sqlite3Binary: WasmSqliteConfiguration,
    ): WasmSQLiteDriver<GraalvmRuntime> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return WasmSQLiteDriver(GraalvmSqliteEmbedder, context) {
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
