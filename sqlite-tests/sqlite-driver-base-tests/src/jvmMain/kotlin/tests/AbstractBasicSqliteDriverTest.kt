/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.test.base.tests

import androidx.sqlite.SQLiteConnection
import assertk.assertThat
import assertk.assertions.containsExactly
import co.touchlab.kermit.Severity
import org.junit.jupiter.api.Test
import ru.pixnews.wasm.sqlite.driver.test.base.AbstractSqliteDriverTest
import ru.pixnews.wasm.sqlite.driver.test.base.TestSqliteDriverCreator
import ru.pixnews.wasm.sqlite.driver.test.base.util.execSQL
import ru.pixnews.wasm.sqlite.driver.test.base.util.queryForString
import ru.pixnews.wasm.sqlite.driver.test.base.util.queryTable
import ru.pixnews.wasm.sqlite.driver.test.base.util.use
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import java.io.File

public abstract class AbstractBasicSqliteDriverTest<E : SqliteEmbedderConfig>(
    driverCreator: TestSqliteDriverCreator,
    dbLoggerSeverity: Severity = Severity.Info,
) : AbstractSqliteDriverTest<E>(
    driverCreator = driverCreator,
    dbLoggerSeverity = dbLoggerSeverity,
) {
    @Test
    public open fun Driver_initialization_should_work() {
        val driver = createWasmSQLiteDriver()
        val connection = driver.open("user.db")
        connection.use(::testDb)
    }

    @Test
    public open fun Driver_initialization_with_in_memory_database_should_work() {
        val driver = createWasmSQLiteDriver(tempDir = File("/nonexistent"))
        val connection = driver.open(":memory:")
        connection.use(::testDb)
    }

    private fun testDb(db: SQLiteConnection) {
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
