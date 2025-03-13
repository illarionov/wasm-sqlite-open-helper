/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.driver.test.base.tests.room

import androidx.sqlite.SQLiteDriver
import at.released.wasm.sqlite.driver.test.base.room.User
import at.released.wasm.sqlite.driver.test.base.room.UserDatabaseSuspend
import at.released.wasm.sqlite.driver.test.base.tests.TestSqliteDriverFactory
import at.released.weh.common.api.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.CoroutineContext

class UserDatabaseTests<S>(
    private val driverFactory: TestSqliteDriverFactory<S>,
    private val databaseFactory: UserDatabaseFactory,
    val logger: Logger,
    val dbLogger: Logger,
) where S : SQLiteDriver, S : AutoCloseable {
    public suspend fun testRoomOnUserDatabase(
        databaseName: String?,
        queryCoroutineContext: CoroutineContext,
        block: suspend (UserDatabaseSuspend) -> Unit,
    ) {
        val driver: S = driverFactory.create(dbLogger, driverFactory.defaultSqliteBinary)
        driver.use {
            testRoomOnUserDatabase(driver, databaseName, queryCoroutineContext, block)
        }
    }

    suspend fun testRoomOnUserDatabase(
        driver: S,
        databaseName: String?,
        coroutineContext: CoroutineContext,
        block: suspend (UserDatabaseSuspend) -> Unit,
    ) {
        val database = databaseFactory.create(driver, databaseName, coroutineContext)
        try {
            block(database)
        } catch (@Suppress("TooGenericExceptionCaught") ex: Throwable) {
            @Suppress("InstanceOfCheckForException")
            if (ex is CancellationException) {
                currentCoroutineContext().ensureActive()
            }
            throw RoomTestFailedException("Room test failed", ex)
        } finally {
            database.close()
        }
    }

    @Suppress("MagicNumber")
    suspend fun basicRoomTest(database: UserDatabaseSuspend) {
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

    public class RoomTestFailedException(
        message: String,
        parent: Throwable,
    ) : RuntimeException(message, parent)

    public fun interface UserDatabaseFactory {
        fun create(
            driver: SQLiteDriver,
            databaseName: String?,
            queryCoroutineContext: CoroutineContext,
        ): UserDatabaseSuspend
    }
}
