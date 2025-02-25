/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.graalvm

import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import ru.pixnews.wasm.sqlite.binary.SqliteAndroidWasmEmscriptenIcu349
import ru.pixnews.wasm.sqlite.driver.WasmSQLiteDriver
import ru.pixnews.wasm.sqlite.driver.test.base.tests.AbstractBasicSqliteDriverTest

@RunWith(Enclosed::class)
class GraalvmBaseSqliteDriverTest {
    class MultiThreadedSqliteTest : AbstractBasicSqliteDriverTest<WasmSQLiteDriver<*>>(
        driverCreator = GraalvmSqliteDriverFactory(),
    ) {
        @JvmField
        @Rule
        val tempFolder: TemporaryFolder = TemporaryFolder()

        override fun fileInTempDir(databaseName: String): String = tempFolder.root.resolve(databaseName).path

        @Test
        override fun Driver_initialization_should_work() {
            super.Driver_initialization_should_work()
        }
    }

    class SingleThreadedSqliteTest : AbstractBasicSqliteDriverTest<WasmSQLiteDriver<*>>(
        driverCreator = GraalvmSqliteDriverFactory(
            defaultSqliteBinary = SqliteAndroidWasmEmscriptenIcu349,
        ),
    ) {
        @JvmField
        @Rule
        val tempFolder: TemporaryFolder = TemporaryFolder()

        override fun fileInTempDir(databaseName: String): String = tempFolder.root.resolve(databaseName).path
    }
}
