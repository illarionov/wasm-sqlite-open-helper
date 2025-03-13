/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.driver.graalvm

import at.released.wasm.sqlite.driver.WasmSQLiteDriver
import at.released.wasm.sqlite.driver.base.JvmDatabaseFactory
import at.released.wasm.sqlite.driver.test.base.tests.AbstractSqliteDriverTest
import at.released.wasm.sqlite.driver.test.base.tests.room.UserDatabaseTests
import at.released.wasm.sqlite.open.helper.graalvm.GraalvmRuntime
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.Executors

class GraalvmBasicMultithreadingRoomTest : AbstractSqliteDriverTest<WasmSQLiteDriver<GraalvmRuntime>>(
    driverFactory = GraalvmSqliteDriverFactory(),
) {
    @get:Rule
    val timeout = CoroutinesTimeout.seconds(60)

    @JvmField
    @Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()
    private val databaseFactory = JvmDatabaseFactory
    val tests = UserDatabaseTests(driverFactory, databaseFactory, logger, dbLogger)

    override fun fileInTempDir(databaseName: String): String = tempFolder.root.resolve(databaseName).path

    @Test
    @Ignore("TODO: flaky")
    fun Test_Room_Multithread() = runTest {
        val driver = driverFactory.create(dbLogger, driverFactory.defaultSqliteBinary)
        driver.use {
            Executors.newFixedThreadPool(
                2,
                driver.runtime.managedThreadFactory,
            ).asCoroutineDispatcher().use { dispatcher ->
                tests.testRoomOnUserDatabase(
                    driver = driver,
                    databaseName = fileInTempDir("test.db"),
                    coroutineContext = dispatcher,
                    block = tests::basicRoomTest,
                )
            }
        }
    }

    @Test
    fun Test_In_Memory_Room_Multithread() = runTest {
        val driver = driverFactory.create(dbLogger, driverFactory.defaultSqliteBinary)
        driver.use {
            Executors.newFixedThreadPool(
                2,
                driver.runtime.managedThreadFactory,
            ).asCoroutineDispatcher().use { dispatcher ->
                tests.testRoomOnUserDatabase(
                    driver = driver,
                    databaseName = null,
                    coroutineContext = dispatcher,
                    block = tests::basicRoomTest,
                )
            }
        }
    }
}
