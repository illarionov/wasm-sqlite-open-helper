/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.tests

import androidx.sqlite.db.SupportSQLiteDatabase
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Severity.Info
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.pixnews.wasm.sqlite.open.helper.base.AbstractOpenHelperFactoryTest
import ru.pixnews.wasm.sqlite.open.helper.base.TestOpenHelperFactoryCreator
import ru.pixnews.wasm.sqlite.open.helper.base.util.readValues
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig

abstract class AbstractIcuCollationTest<E : SqliteEmbedderConfig>(
    factoryCreator: TestOpenHelperFactoryCreator<E>,
    dbLoggerSeverity: Severity = Info,
) : AbstractOpenHelperFactoryTest<E>(
    factoryCreator = factoryCreator,
    dbLoggerSeverity = dbLoggerSeverity,
) {
    lateinit var database: SupportSQLiteDatabase

    @BeforeEach
    fun setup() {
        val helper = createWasmSQLiteOpenHelper()
        database = helper.writableDatabase
    }

    @AfterEach
    fun destroy() {
        database.close()
    }

    @Test
    fun `Icu uppercase() should work`() {
        val upperUser = database.compileStatement("""SELECT upper('пользователь 1', 'ru_RU')""").use {
            it.simpleQueryForString()
        }
        assertThat(upperUser).isEqualTo("ПОЛЬЗОВАТЕЛЬ 1")
    }

    @Test
    fun `Icu lowercase() should work`() {
        val rows: List<String?> = database.query("""SELECT lower('ISPANAK', 'tr_tr')""")
            .readValues()
            .map { row: Map<String, String?> -> row.values.single() }

        assertThat(rows).containsExactly("ıspanak")
    }

    @Test
    fun `Case-insensitive LIKE should work`() {
        val caseInsensitiveLike = database.compileStatement(
            """SELECT 'тамга сезимсиз текст издөө' LIKE '%ЕЗИМС%'""",
        ).use {
            it.simpleQueryForLong()
        }
        assertThat(caseInsensitiveLike).isEqualTo(1)
    }

    @Test
    fun `Icu Collation should work`() {
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
