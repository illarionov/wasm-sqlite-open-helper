/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.test.base.tests.room

import co.touchlab.kermit.Severity
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.runTest
import ru.pixnews.wasm.sqlite.driver.test.base.tests.AbstractSqliteDriverTest
import ru.pixnews.wasm.sqlite.driver.test.base.tests.TestSqliteDriverFactory
import ru.pixnews.wasm.sqlite.driver.test.base.tests.room.UserDatabaseTests.UserDatabaseFactory
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import java.util.concurrent.Executors
import kotlin.test.Test

abstract class AbstractBasicMultithreadingRoomTest<E : SqliteEmbedderConfig>(
    driverFactory: TestSqliteDriverFactory,
    val databaseFactory: UserDatabaseFactory,
    dbLoggerSeverity: Severity = Severity.Info,
) : AbstractSqliteDriverTest<E>(driverFactory, dbLoggerSeverity) {
    val tests = UserDatabaseTests(driverFactory, databaseFactory, logger, dbLogger)

    @Test
    public open fun Test_Room_Multithread() = runTest {
        Executors.newFixedThreadPool(4).asCoroutineDispatcher().use { dispatcher ->
            tests.testRoomOnUserDatabase(
                databaseName = fileInTempDir("test.db"),
                queryCoroutineContext = dispatcher,
                block = tests::basicRoomTest,
            )
        }
    }

    @Test
    public open fun Test_In_Memory_Room_Multithread() = runTest {
        Executors.newFixedThreadPool(4).asCoroutineDispatcher().use { dispatcher ->
            tests.testRoomOnUserDatabase(
                databaseName = null,
                queryCoroutineContext = dispatcher,
                block = tests::basicRoomTest,
            )
        }
    }
}
