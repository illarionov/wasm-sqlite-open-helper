/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.test.base.tests

import androidx.sqlite.SQLiteConnection
import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.startsWith
import co.touchlab.kermit.Severity
import ru.pixnews.wasm.sqlite.driver.test.base.util.queryForLong
import ru.pixnews.wasm.sqlite.driver.test.base.util.queryForString
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

public abstract class AbstractTimeFunctionsTest<E : SqliteEmbedderConfig>(
    driverCreator: TestSqliteDriverFactory,
    dbLoggerSeverity: Severity = Severity.Info,
) : AbstractSqliteDriverTest<E>(
    driverFactory = driverCreator,
    dbLoggerSeverity = dbLoggerSeverity,
) {
    lateinit var connection: SQLiteConnection

    @BeforeTest
    open fun setup() {
        val driver = createWasmSQLiteDriver()
        connection = driver.open("test.db")
    }

    @AfterTest
    open fun destroy() {
        if (::connection.isInitialized) {
            connection.close()
        }
    }

    @Test
    open fun date_should_work() {
        val dateString = connection.queryForString("""SELECT date()""")

        assertThat(dateString).isEqualTo(
            SimpleDateFormat("YYYY-MM-dd").apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date()),
        )
    }

    @Test
    open fun unixepoch_should_work() {
        val timestampNow = Instant.now().epochSecond
        val unixEpoch = connection.queryForLong("""SELECT unixepoch()""") ?: error("Null epoch")

        assertThat(unixEpoch).isBetween(timestampNow, timestampNow + 10.minutes.inWholeSeconds)
    }

    @Test
    open fun localtime_modifier_should_work() {
        val localtimeString = connection.queryForString(
            """SELECT datetime(1092941466, 'unixepoch', 'localtime')""",
        ) ?: ""

        assertThat(localtimeString).startsWith("2004")
    }
}
