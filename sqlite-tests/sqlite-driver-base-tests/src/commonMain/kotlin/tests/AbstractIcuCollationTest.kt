/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.driver.test.base.tests

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import assertk.assertThat
import assertk.assertions.isEqualTo
import at.released.wasm.sqlite.driver.test.base.util.execSQL
import at.released.wasm.sqlite.driver.test.base.util.queryForLong
import at.released.wasm.sqlite.driver.test.base.util.queryForString
import co.touchlab.kermit.Severity
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

public abstract class AbstractIcuCollationTest<S>(
    driverCreator: TestSqliteDriverFactory<S>,
    dbLoggerSeverity: Severity = Severity.Info,
) : AbstractSqliteDriverTest<S>(
    driverFactory = driverCreator,
    dbLoggerSeverity = dbLoggerSeverity,
) where S : SQLiteDriver, S : AutoCloseable {
    lateinit var driver: S
    lateinit var connection: SQLiteConnection

    @BeforeTest
    open fun setup() {
        driver = createWasmSQLiteDriver()
        connection = driver.open(fileInTempDir("test.db"))
    }

    @AfterTest
    open fun destroy() {
        if (::connection.isInitialized) {
            connection.close()
        }
        if (::driver.isInitialized) {
            driver.close()
        }
    }

    @Test
    open fun Icu_uppercase_should_work() {
        val upperUser = connection.queryForString("""SELECT upper('пользователь 1', 'ru_RU')""")

        assertThat(upperUser).isEqualTo("ПОЛЬЗОВАТЕЛЬ 1")
    }

    @Test
    open fun Icu_lowercase_should_work() {
        val lowerUser = connection.queryForString("""SELECT lower('ISPANAK', 'tr_tr')""")

        assertThat(lowerUser).isEqualTo("ıspanak")
    }

    @Test
    open fun Case_insensitive_LIKE_should_work() {
        val caseInsensitiveLike = connection.queryForLong(
            """SELECT 'тамга сезимсиз текст издөө' LIKE '%ЕЗИМС%'""",
        )
        assertThat(caseInsensitiveLike).isEqualTo(1)
    }

    @Test
    open fun Icu_Collation_should_work() {
        connection.execSQL("SELECT icu_load_collation('tr_TR', 'turkish')")
        connection.execSQL("""CREATE TABLE Penpal(name TEXT COLLATE turkish)""".trimIndent())
        connection.execSQL(
            "INSERT INTO Penpal(`name`) VALUES (?)",
            "Nazlı",
        )

        val name = connection.queryForString("SELECT name FROM Penpal")

        assertThat(name).isEqualTo("Nazlı")
    }
}
