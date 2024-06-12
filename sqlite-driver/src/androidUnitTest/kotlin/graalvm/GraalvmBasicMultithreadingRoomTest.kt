/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.graalvm

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.pixnews.wasm.sqlite.driver.WasmSQLiteDriver
import ru.pixnews.wasm.sqlite.driver.base.AndroidUserDatabaseSuspendFactory
import ru.pixnews.wasm.sqlite.driver.test.base.tests.AbstractSqliteDriverTest
import ru.pixnews.wasm.sqlite.driver.test.base.tests.room.UserDatabaseTests
import ru.pixnews.wasm.sqlite.open.helper.graalvm.GraalvmRuntimeInstance
import java.util.concurrent.Executors

class GraalvmBasicMultithreadingRoomTest : AbstractSqliteDriverTest<WasmSQLiteDriver<GraalvmRuntimeInstance>>(
    driverFactory = GraalvmSqliteDriverFactory(),
) {
    @JvmField
    @Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()
    private val databaseFactory = AndroidUserDatabaseSuspendFactory
    val tests = UserDatabaseTests(driverFactory, databaseFactory, logger, dbLogger)

    override fun fileInTempDir(databaseName: String): String = tempFolder.root.resolve(databaseName).path

    @Test
    @Ignore("TODO: Fix")
    fun Test_Room_Multithread() = runTest {
        val driver = driverFactory.create(dbLogger, driverFactory.defaultSqliteBinary)
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

    @Test
    fun Test_In_Memory_Room_Multithread() = runTest {
        val driver = driverFactory.create(dbLogger, driverFactory.defaultSqliteBinary)
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
