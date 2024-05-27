/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.tests

import androidx.sqlite.SQLiteConnection
import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.startsWith
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Severity.Info
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.pixnews.wasm.sqlite.driver.base.AbstractSqliteDriverTest
import ru.pixnews.wasm.sqlite.driver.base.TestSqliteDriverCreator
import ru.pixnews.wasm.sqlite.driver.test.base.util.queryForLong
import ru.pixnews.wasm.sqlite.driver.test.base.util.queryForString
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.TimeZone
import kotlin.time.Duration.Companion.minutes

abstract class AbstractTimeFunctionsTest<E : SqliteEmbedderConfig>(
    driverCreator: TestSqliteDriverCreator<E>,
    dbLoggerSeverity: Severity = Info,
) : AbstractSqliteDriverTest<E>(
    driverCreator = driverCreator,
    dbLoggerSeverity = dbLoggerSeverity,
) {
    lateinit var connection: SQLiteConnection

    @BeforeEach
    fun setup() {
        val driver = createWasmSQLiteDriver()
        connection = driver.open("test.db")
    }

    @AfterEach
    fun destroy() {
        connection.close()
    }

    @Test
    fun `date() should work`() {
        val dateString = connection.queryForString("""SELECT date()""")

        assertThat(dateString).isEqualTo(
            SimpleDateFormat("YYYY-MM-dd").apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date()),
        )
    }

    @Test
    fun `unixepoch() should work`() {
        val timestampNow = Instant.now().epochSecond
        val unixEpoch = connection.queryForLong("""SELECT unixepoch()""") ?: error("Null epoch")

        assertThat(unixEpoch).isBetween(timestampNow, timestampNow + 10.minutes.inWholeSeconds)
    }

    @Test
    fun `localtime modifier should work`() {
        val localtimeString = connection.queryForString(
            """SELECT datetime(1092941466, 'unixepoch', 'localtime')""",
        ) ?: ""

        assertThat(localtimeString).startsWith("2004")
    }
}
