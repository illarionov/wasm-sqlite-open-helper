/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.test.base.tests.room

import androidx.sqlite.SQLiteDriver
import ru.pixnews.wasm.sqlite.driver.test.base.room.User
import ru.pixnews.wasm.sqlite.driver.test.base.room.UserDatabaseSuspend
import ru.pixnews.wasm.sqlite.driver.test.base.tests.TestSqliteDriverFactory
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import kotlin.coroutines.CoroutineContext

class UserDatabaseTests(
    private val driverFactory: TestSqliteDriverFactory,
    private val databaseFactory: UserDatabaseFactory,
    val logger: Logger,
    val dbLogger: Logger,
) {
    internal suspend fun testRoomOnUserDatabase(
        databaseName: String?,
        queryCoroutineContext: CoroutineContext,
        block: suspend (UserDatabaseSuspend) -> Unit,
    ) {
        val driver = driverFactory.create(dbLogger, driverFactory.defaultSqliteBinary)
        val database = databaseFactory.create(driver, databaseName, queryCoroutineContext)
        try {
            block(database)
        } finally {
            database.close()
        }
    }

    @Suppress("MagicNumber")
    internal suspend fun basicRoomTest(database: UserDatabaseSuspend) {
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
