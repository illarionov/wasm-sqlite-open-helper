/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.test.base.tests

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.startsWith
import co.touchlab.kermit.Severity
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import ru.pixnews.wasm.sqlite.driver.test.base.util.queryForLong
import ru.pixnews.wasm.sqlite.driver.test.base.util.queryForString
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

public abstract class AbstractTimeFunctionsTest<S>(
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
        val driver = createWasmSQLiteDriver()
        connection = driver.open(fileInTempDir("test.db"))
    }

    @AfterTest
    open fun destroy() {
        if (::connection.isInitialized) {
            connection.close()
        }
    }

    @Test
    open fun date_should_work() {
        val nowDateUtc = Clock.System.todayIn(TimeZone.UTC).toString()
        val dateString = connection.queryForString("""SELECT date()""")

        assertThat(dateString).isEqualTo(nowDateUtc)
    }

    @Test
    open fun unixepoch_should_work() {
        val timestampNow = Clock.System.now().epochSeconds
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
