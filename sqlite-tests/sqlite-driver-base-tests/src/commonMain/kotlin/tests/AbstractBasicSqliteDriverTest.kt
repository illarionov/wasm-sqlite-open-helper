/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.driver.test.base.tests

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import assertk.assertThat
import assertk.assertions.containsExactly
import at.released.wasm.sqlite.driver.test.base.util.execSQL
import at.released.wasm.sqlite.driver.test.base.util.queryForString
import at.released.wasm.sqlite.driver.test.base.util.queryTable
import at.released.wasm.sqlite.driver.test.base.util.use
import co.touchlab.kermit.Severity
import kotlin.test.Test

public abstract class AbstractBasicSqliteDriverTest<S>(
    driverCreator: TestSqliteDriverFactory<S>,
    dbLoggerSeverity: Severity = Severity.Info,
) : AbstractSqliteDriverTest<S>(
    driverFactory = driverCreator,
    dbLoggerSeverity = dbLoggerSeverity,
) where S : SQLiteDriver, S : AutoCloseable {
    @Test
    public open fun Driver_initialization_should_work() = createWasmSQLiteDriver().use { driver ->
        val connection = driver.open(fileInTempDir("user.db"))
        connection.use(::testDb)
    }

    @Test
    public open fun Driver_initialization_with_in_memory_database_should_work() {
        createWasmSQLiteDriver().use { driver ->
            val connection = driver.open(":memory:")
            connection.use(::testDb)
        }
    }

    public fun testDb(db: SQLiteConnection) {
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
