/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.test.base.tests

import androidx.sqlite.db.SupportSQLiteDatabase
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Severity.Info
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import ru.pixnews.wasm.sqlite.open.helper.test.base.AbstractOpenHelperFactoryTest
import ru.pixnews.wasm.sqlite.open.helper.test.base.TestOpenHelperFactoryCreator
import ru.pixnews.wasm.sqlite.open.helper.test.base.util.readValues
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

abstract class AbstractIcuCollationTest<E : SqliteEmbedderConfig>(
    factoryCreator: TestOpenHelperFactoryCreator,
    dbLoggerSeverity: Severity = Info,
) : AbstractOpenHelperFactoryTest<E>(
    factoryCreator = factoryCreator,
    dbLoggerSeverity = dbLoggerSeverity,
) {
    lateinit var database: SupportSQLiteDatabase

    @BeforeTest
    fun setup() {
        val helper = createWasmSQLiteOpenHelper()
        database = helper.writableDatabase
    }

    @AfterTest
    fun destroy() {
        database.close()
    }

    @Test
    fun Icu_uppercase_should_work() {
        val upperUser = database.compileStatement("""SELECT upper('пользователь 1', 'ru_RU')""").use {
            it.simpleQueryForString()
        }
        assertThat(upperUser).isEqualTo("ПОЛЬЗОВАТЕЛЬ 1")
    }

    @Test
    fun Icu_lowercase_should_work() {
        val rows: List<String?> = database.query("""SELECT lower('ISPANAK', 'tr_tr')""")
            .readValues()
            .map { row: Map<String, String?> -> row.values.single() }

        assertThat(rows).containsExactly("ıspanak")
    }

    @Test
    fun Case_insensitive_LIKE_should_work() {
        val caseInsensitiveLike = database.compileStatement(
            """SELECT 'тамга сезимсиз текст издөө' LIKE '%ЕЗИМС%'""",
        ).use {
            it.simpleQueryForLong()
        }
        assertThat(caseInsensitiveLike).isEqualTo(1)
    }

    @Test
    fun Icu_Collation_should_work() {
        database.query("SELECT icu_load_collation('tr_TR', 'turkish')").readValues()
        database.execSQL("""CREATE TABLE Penpal(name TEXT COLLATE turkish)""".trimIndent())

        database.execSQL(
            "INSERT INTO Penpal(`name`) VALUES (?)",
            arrayOf("Nazlı"),
        )
        val names: List<String?> = database.query("SELECT name FROM Penpal")
            .readValues()
            .map { row: Map<String, String?> -> row.values.single() }

        assertThat(names).containsExactly("Nazlı")
    }
}
