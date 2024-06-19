/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.test.base.tests.room

import androidx.sqlite.SQLiteDriver
import co.touchlab.kermit.Severity
import kotlinx.coroutines.test.runTest
import ru.pixnews.wasm.sqlite.driver.test.base.tests.AbstractSqliteDriverTest
import ru.pixnews.wasm.sqlite.driver.test.base.tests.TestSqliteDriverFactory
import ru.pixnews.wasm.sqlite.driver.test.base.tests.room.UserDatabaseTests.UserDatabaseFactory
import kotlin.test.Test

public abstract class AbstractBasicRoomTest<S : SQLiteDriver>(
    driverFactory: TestSqliteDriverFactory<S>,
    databaseFactory: UserDatabaseFactory,
    dbLoggerSeverity: Severity = Severity.Info,
) : AbstractSqliteDriverTest<S>(
    driverFactory = driverFactory,
    dbLoggerSeverity = dbLoggerSeverity,
) {
    val tests = UserDatabaseTests(driverFactory, databaseFactory, logger, dbLogger)

    @Test
    open fun Test_Room() = runTest {
        tests.testRoomOnUserDatabase(
            databaseName = fileInTempDir("test.db"),
            queryCoroutineContext = coroutineContext,
            block = tests::basicRoomTest,
        )
    }

    @Test
    open fun Test_In_Memory_Room() = runTest {
        tests.testRoomOnUserDatabase(
            databaseName = null,
            queryCoroutineContext = this.coroutineContext,
            block = tests::basicRoomTest,
        )
    }
}
