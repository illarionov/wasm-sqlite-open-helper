/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.test.base.tests

import androidx.sqlite.SQLiteDriver
import co.touchlab.kermit.Severity
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import ru.pixnews.wasm.sqlite.driver.test.base.AbstractSqliteDriverTest
import ru.pixnews.wasm.sqlite.driver.test.base.TestSqliteDriverCreator
import ru.pixnews.wasm.sqlite.driver.test.base.room.User
import ru.pixnews.wasm.sqlite.driver.test.base.room.UserDatabaseSuspend
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import kotlin.coroutines.CoroutineContext

abstract class AbstractBasicRoomTest<E : SqliteEmbedderConfig>(
    driverCreator: TestSqliteDriverCreator,
    val databaseFactory: (driver: SQLiteDriver, queryCoroutineContext: CoroutineContext) -> UserDatabaseSuspend,
    dbLoggerSeverity: Severity = Severity.Info,
) : AbstractSqliteDriverTest<E>(
    driverCreator = driverCreator,
    dbLoggerSeverity = dbLoggerSeverity,
) {
    @Test
    @Suppress("MagicNumber")
    public open fun `Test Room`() = runTest {
        val driver = createWasmSQLiteDriver()
        val db = databaseFactory(driver, this.backgroundScope.coroutineContext)
        val userDao = db.userDao()

        val user101 = User(101, "User 101 First Name", "User 101 Last Name")
        userDao.insertAll(
            User(100, "User 100 First Name", "User 100 Last Name"),
            user101,
            User(102, "User 102 First Name", "User 102 Last Name"),
        )
        userDao.delete(user101)

        val usersByIds = userDao.loadAllByIds(intArrayOf(101, 102))
        val userByName = userDao.findByName("User 102 First Name", "User 102 Last Name")
        val users: List<User> = userDao.getAll()

        logger.i { "users by ids: $usersByIds; user by name: $userByName; users: $users;" }

        db.close()
    }
}
