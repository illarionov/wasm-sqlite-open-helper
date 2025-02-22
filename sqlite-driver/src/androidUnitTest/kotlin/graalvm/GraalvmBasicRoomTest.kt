/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.graalvm

import kotlinx.coroutines.test.TestResult
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import ru.pixnews.wasm.sqlite.binary.SqliteAndroidWasmEmscriptenIcu348
import ru.pixnews.wasm.sqlite.driver.WasmSQLiteDriver
import ru.pixnews.wasm.sqlite.driver.base.AndroidUserDatabaseSuspendFactory
import ru.pixnews.wasm.sqlite.driver.test.base.tests.room.AbstractBasicRoomTest

@RunWith(Enclosed::class)
class GraalvmBasicRoomTest {
    class MultithreadingSqliteTest : AbstractBasicRoomTest<WasmSQLiteDriver<*>>(
        driverFactory = GraalvmSqliteDriverFactory(),
        databaseFactory = AndroidUserDatabaseSuspendFactory,
    ) {
        @JvmField
        @Rule
        val tempFolder: TemporaryFolder = TemporaryFolder()

        override fun fileInTempDir(databaseName: String): String = tempFolder.root.resolve(databaseName).path

        @Test
        override fun Test_Room(): TestResult {
            return super.Test_Room()
        }
    }

    class SingleThreadedSqliteTest : AbstractBasicRoomTest<WasmSQLiteDriver<*>>(
        driverFactory = GraalvmSqliteDriverFactory(
            defaultSqliteBinary = SqliteAndroidWasmEmscriptenIcu348,
        ),
        databaseFactory = AndroidUserDatabaseSuspendFactory,
    ) {
        @JvmField
        @Rule
        val tempFolder: TemporaryFolder = TemporaryFolder()

        override fun fileInTempDir(databaseName: String): String = tempFolder.root.resolve(databaseName).path
    }
}
