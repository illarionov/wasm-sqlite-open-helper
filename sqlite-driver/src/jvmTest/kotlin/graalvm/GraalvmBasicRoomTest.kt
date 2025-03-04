/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.graalvm

import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import org.junit.Rule
import org.junit.experimental.runners.Enclosed
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import ru.pixnews.wasm.sqlite.binary.SqliteAndroidWasmEmscriptenIcu349
import ru.pixnews.wasm.sqlite.driver.WasmSQLiteDriver
import ru.pixnews.wasm.sqlite.driver.base.JvmDatabaseFactory
import ru.pixnews.wasm.sqlite.driver.test.base.tests.room.AbstractBasicRoomTest

@RunWith(Enclosed::class)
class GraalvmBasicRoomTest {
    class MultithreadingSqliteTest : AbstractBasicRoomTest<WasmSQLiteDriver<*>>(
        driverFactory = GraalvmSqliteDriverFactory(),
        databaseFactory = JvmDatabaseFactory,
    ) {
        @get:Rule
        public val timeout = CoroutinesTimeout.seconds(60)

        @JvmField
        @Rule
        val tempFolder: TemporaryFolder = TemporaryFolder()

        override fun fileInTempDir(databaseName: String): String = tempFolder.root.resolve(databaseName).path
    }

    class SingleThreadedSqliteTest : AbstractBasicRoomTest<WasmSQLiteDriver<*>>(
        driverFactory = GraalvmSqliteDriverFactory(
            defaultSqliteBinary = SqliteAndroidWasmEmscriptenIcu349,
        ),
        databaseFactory = JvmDatabaseFactory,
    ) {
        @get:Rule
        public val timeout = CoroutinesTimeout.seconds(60)

        @JvmField
        @Rule
        val tempFolder: TemporaryFolder = TemporaryFolder()

        override fun fileInTempDir(databaseName: String): String = tempFolder.root.resolve(databaseName).path
    }
}
