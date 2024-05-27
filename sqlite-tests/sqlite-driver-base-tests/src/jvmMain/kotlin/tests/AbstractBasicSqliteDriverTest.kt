/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.test.base.tests

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import assertk.assertThat
import assertk.assertions.containsExactly
import co.touchlab.kermit.Severity
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import ru.pixnews.wasm.sqlite.driver.test.base.AbstractSqliteDriverTest
import ru.pixnews.wasm.sqlite.driver.test.base.TestSqliteDriverCreator
import ru.pixnews.wasm.sqlite.driver.test.base.room.User
import ru.pixnews.wasm.sqlite.driver.test.base.room.UserDatabaseSuspend
import ru.pixnews.wasm.sqlite.driver.test.base.util.execSQL
import ru.pixnews.wasm.sqlite.driver.test.base.util.queryForString
import ru.pixnews.wasm.sqlite.driver.test.base.util.queryTable
import ru.pixnews.wasm.sqlite.driver.test.base.util.use
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig

public abstract class AbstractBasicSqliteDriverTest<E : SqliteEmbedderConfig>(
    driverCreator: TestSqliteDriverCreator,
    public val databaseFactory: TestScope.(driver: SQLiteDriver) -> UserDatabaseSuspend,
    dbLoggerSeverity: Severity = Severity.Info,
) : AbstractSqliteDriverTest<E>(
    driverCreator = driverCreator,
    dbLoggerSeverity = dbLoggerSeverity,
) {
    @Test
    public open fun `Driver initialization should work`() {
        val driver = createWasmSQLiteDriver()
        val connection = driver.open("user.db")
        connection.use { db: SQLiteConnection ->
            val version = db.queryForString("SELECT sqlite_version()")
            logger.i { "Version: $version" }

            db.execSQL("CREATE TABLE IF NOT EXISTS User(id INTEGER PRIMARY KEY, name TEXT)")
            db.execSQL(
                "INSERT INTO User(`name`) VALUES (?), (?), (?)",
                "user 1",
                "user 2",
                "user 3",
            )
            val users = db.queryTable("SELECT * FROM User")
            logger.i { "users: $users" }
            assertThat(users).containsExactly(
                mapOf("id" to "1", "name" to "user 1"),
                mapOf("id" to "2", "name" to "user 2"),
                mapOf("id" to "3", "name" to "user 3"),
            )
        }
    }

    @Test
    @Suppress("MagicNumber")
    public open fun `Test Room`(): TestResult = runTest {
        val driver = createWasmSQLiteDriver()
        val db = databaseFactory(driver)
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
