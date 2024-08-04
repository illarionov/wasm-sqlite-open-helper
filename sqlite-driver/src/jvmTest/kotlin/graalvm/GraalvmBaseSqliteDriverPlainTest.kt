/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.graalvm

import androidx.sqlite.SQLiteDriver
import org.junit.Rule
import org.junit.experimental.runners.Enclosed
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import ru.pixnews.wasm.sqlite.binary.SqliteWasmEmscripten346
import ru.pixnews.wasm.sqlite.binary.SqliteWasmEmscriptenMtPthread346
import ru.pixnews.wasm.sqlite.driver.test.base.tests.AbstractBasicSqliteDriverTest

/**
 * Tests with SQLite build without ICU and Android extensions
 */
@RunWith(Enclosed::class)
class GraalvmBaseSqliteDriverPlainTest {
    class MultiThreadedSqlitePlainTest : AbstractBasicSqliteDriverTest<SQLiteDriver>(
        driverCreator = GraalvmSqliteDriverFactory(
            defaultSqliteBinary = SqliteWasmEmscriptenMtPthread346,
            additionalConfig = {
                openParams {
                    openFlags = setOf()
                }
            },
        ),
    ) {
        @JvmField
        @Rule
        val tempFolder: TemporaryFolder = TemporaryFolder()

        override fun fileInTempDir(databaseName: String): String = tempFolder.root.resolve(databaseName).path
    }

    class SingleThreadedSqlitePlainTest : AbstractBasicSqliteDriverTest<SQLiteDriver>(
        driverCreator = GraalvmSqliteDriverFactory(
            defaultSqliteBinary = SqliteWasmEmscripten346,
            additionalConfig = {
                openParams {
                    openFlags = setOf()
                }
            },
        ),
    ) {
        @JvmField
        @Rule
        val tempFolder: TemporaryFolder = TemporaryFolder()

        override fun fileInTempDir(databaseName: String): String = tempFolder.root.resolve(databaseName).path
    }
}
