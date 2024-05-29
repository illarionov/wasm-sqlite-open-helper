/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MagicNumber")

package ru.pixnews.wasm.sqlite.driver.test.base.tests

import androidx.sqlite.SQLiteDriver
import co.touchlab.kermit.Severity
import kotlinx.coroutines.test.runTest
import ru.pixnews.wasm.sqlite.driver.test.base.room.User
import ru.pixnews.wasm.sqlite.driver.test.base.room.UserDatabaseSuspend
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test

public abstract class AbstractBasicRoomTest<E : SqliteEmbedderConfig>(
    driverCreator: TestSqliteDriverFactory,
    val databaseFactory: UserDatabaseFactory,
    dbLoggerSeverity: Severity = Severity.Info,
) : AbstractSqliteDriverTest<E>(
    driverFactory = driverCreator,
    dbLoggerSeverity = dbLoggerSeverity,
) {
    @Test
    public open fun Test_Room() = runTest {
        val driver = createWasmSQLiteDriver()
        val database = databaseFactory.create(driver, "test.db", this.backgroundScope.coroutineContext)
        try {
            testRoom(database)
        } finally {
            database.close()
        }
    }

    @Test
    public open fun Test_In_Memory_Room() = runTest {
        val driver = createWasmSQLiteDriver(tempDir = "/nonexistent")
        val database = databaseFactory.create(driver, null, this.backgroundScope.coroutineContext)
        try {
            testRoom(database)
        } finally {
            database.close()
        }
    }

    private suspend fun testRoom(database: UserDatabaseSuspend) {
        val userDao = database.userDao()

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
    }

    public fun interface UserDatabaseFactory {
        fun create(
            driver: SQLiteDriver,
            databaseName: String?,
            queryCoroutineContext: CoroutineContext,
        ): UserDatabaseSuspend
    }
}
