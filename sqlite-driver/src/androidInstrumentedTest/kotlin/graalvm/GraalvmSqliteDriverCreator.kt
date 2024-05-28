/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.graalvm

import androidx.sqlite.SQLiteDriver
import org.graalvm.polyglot.Engine
import ru.pixnews.wasm.sqlite.driver.WasmSQLiteDriver
import ru.pixnews.wasm.sqlite.driver.base.defaultTestSqliteDriverConfig
import ru.pixnews.wasm.sqlite.driver.test.base.TestSqliteDriverCreator
import ru.pixnews.wasm.sqlite.open.helper.SqliteAndroidWasmEmscriptenIcuMtPthread346
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.graalvm.GraalvmSqliteEmbedder
import java.io.File

class GraalvmSqliteDriverCreator(
    private val initialGraalvmEngine: Engine? = WASM_GRAALVM_ENGINE,
    override val defaultSqliteBinary: WasmSqliteConfiguration = SqliteAndroidWasmEmscriptenIcuMtPthread346,
) : TestSqliteDriverCreator {
    override fun create(dstDir: File, dbLogger: Logger, sqlite3Binary: WasmSqliteConfiguration): SQLiteDriver {
        return WasmSQLiteDriver(GraalvmSqliteEmbedder) {
            defaultTestSqliteDriverConfig(dstDir, dbLogger)
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
