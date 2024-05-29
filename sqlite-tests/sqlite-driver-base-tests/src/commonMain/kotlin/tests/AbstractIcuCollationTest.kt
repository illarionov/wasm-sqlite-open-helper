/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.test.base.tests

import androidx.sqlite.SQLiteConnection
import assertk.assertThat
import assertk.assertions.isEqualTo
import co.touchlab.kermit.Severity
import ru.pixnews.wasm.sqlite.driver.test.base.util.execSQL
import ru.pixnews.wasm.sqlite.driver.test.base.util.queryForLong
import ru.pixnews.wasm.sqlite.driver.test.base.util.queryForString
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

public abstract class AbstractIcuCollationTest<E : SqliteEmbedderConfig>(
    driverCreator: TestSqliteDriverFactory,
    dbLoggerSeverity: Severity = Severity.Info,
) : AbstractSqliteDriverTest<E>(
    driverFactory = driverCreator,
    dbLoggerSeverity = dbLoggerSeverity,
) {
    lateinit var connection: SQLiteConnection

    @BeforeTest
    fun setup() {
        val driver = createWasmSQLiteDriver()
        connection = driver.open("test.db")
    }

    @AfterTest
    fun destroy() {
        connection.close()
    }

    @Test
    fun Icu_uppercase_should_work() {
        val upperUser = connection.queryForString("""SELECT upper('пользователь 1', 'ru_RU')""")

        assertThat(upperUser).isEqualTo("ПОЛЬЗОВАТЕЛЬ 1")
    }

    @Test
    fun Icu_lowercase_should_work() {
        val lowerUser = connection.queryForString("""SELECT lower('ISPANAK', 'tr_tr')""")

        assertThat(lowerUser).isEqualTo("ıspanak")
    }

    @Test
    fun Case_insensitive_LIKE_should_work() {
        val caseInsensitiveLike = connection.queryForLong(
            """SELECT 'тамга сезимсиз текст издөө' LIKE '%ЕЗИМС%'""",
        )
        assertThat(caseInsensitiveLike).isEqualTo(1)
    }

    @Test
    fun Icu_Collation_should_work() {
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
