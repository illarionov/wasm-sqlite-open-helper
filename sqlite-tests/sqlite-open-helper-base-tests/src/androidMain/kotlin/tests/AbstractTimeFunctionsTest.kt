/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.test.base.tests

import androidx.sqlite.db.SupportSQLiteDatabase
import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import assertk.assertions.startsWith
import at.released.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import at.released.wasm.sqlite.open.helper.test.base.AbstractOpenHelperFactoryTest
import at.released.wasm.sqlite.open.helper.test.base.TestOpenHelperFactoryCreator
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Severity.Info
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.TimeZone
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

abstract class AbstractTimeFunctionsTest<E : SqliteEmbedderConfig>(
    factoryCreator: TestOpenHelperFactoryCreator,
    dbLoggerSeverity: Severity = Info,
) : AbstractOpenHelperFactoryTest<E>(
    factoryCreator = factoryCreator,
    dbLoggerSeverity = dbLoggerSeverity,
) {
    lateinit var database: SupportSQLiteDatabase

    @BeforeTest
    open fun setup() {
        val helper = createWasmSQLiteOpenHelper(dbName = null)
        database = helper.writableDatabase
    }

    @AfterTest
    open fun destroy() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    open fun date_should_work() {
        val dateString = database.compileStatement("""SELECT date()""").use {
            it.simpleQueryForString()
        }
        assertThat(dateString).isEqualTo(
            SimpleDateFormat("YYYY-MM-dd").apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date()),
        )
    }

    @Test
    open fun unixepoch_should_work() {
        val timestampNow = Instant.now().epochSecond
        val unixEpoch = database.compileStatement("""SELECT unixepoch()""").use {
            it.simpleQueryForLong()
        }
        assertThat(unixEpoch).isBetween(timestampNow, timestampNow + 10.minutes.inWholeSeconds)
    }

    @Test
    open fun localtime_modifier_should_work() {
        val localtimeString: String = database.compileStatement(
            """SELECT datetime(1092941466, 'unixepoch', 'localtime')""",
        ).use {
            it.simpleQueryForString() ?: ""
        }
        assertThat(localtimeString).startsWith("2004")
    }
}
