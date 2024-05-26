/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.tests

import androidx.sqlite.use
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Severity.Info
import org.junit.jupiter.api.Test
import ru.pixnews.wasm.sqlite.driver.base.AbstractSqliteDriverTest
import ru.pixnews.wasm.sqlite.driver.base.TestSqliteDriverCreator
import ru.pixnews.wasm.sqlite.driver.base.util.execSQL
import ru.pixnews.wasm.sqlite.driver.base.util.queryForString
import ru.pixnews.wasm.sqlite.driver.base.util.readResult
import ru.pixnews.wasm.sqlite.driver.base.util.use
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

abstract class AbstractMultithreadingTest<E : SqliteEmbedderConfig>(
    driverCreator: TestSqliteDriverCreator<E>,
    dbLoggerSeverity: Severity = Info,
) : AbstractSqliteDriverTest<E>(
    driverCreator = driverCreator,
    dbLoggerSeverity = dbLoggerSeverity,
) {
    @Test
    fun `Factory from multiple threads should work`() {
        val driver = createWasmSQLiteDriver()

        driver.open("test.db").use { db ->
            db.execSQL("CREATE TABLE t1(x, y)")
            db.execSQL("INSERT INTO t1 VALUES (1, 2), (3, 4)")

            var thread2Result: TimedValue<String?>? = null

            Thread {
                driver.open("test.db").use { db2 ->
                    thread2Result = measureTimedValue {
                        db2.queryForString("SELECT sum(x+y) FROM t1")
                    }
                }
            }.apply {
                start()
                join(10.seconds.inWholeMilliseconds)
            }

            val (result, time) = checkNotNull(thread2Result)

            assertThat(result).isEqualTo("10")
            logger.i { "Request 2 took $time" }
        }
    }

    @Test
    fun `Factory from multiple threads with active transaction should work`() {
        val driver = createWasmSQLiteDriver()

        driver.open("test.db").use { db ->
            db.execSQL("CREATE TABLE t1(x, y)")
            db.execSQL("INSERT INTO t1 VALUES (1, 2), (3, 4)")

            val newMode = db.queryForString("PRAGMA journal_mode=WAL")
            check("WAL".equals(newMode, ignoreCase = true)) { "Can not change mode to WAL" }

            db.execSQL("BEGIN IMMEDIATE TRANSACTION")
            db.execSQL("INSERT INTO t1 VALUES (5, 6)")

            val innerStatementCompiled = CountDownLatch(1)
            var thread2Result: String? = null

            val backgroundThread = Thread {
                driver.open("test.db").use { db2 ->
                    db2.prepare("SELECT sum(x+y) FROM t1").use {
                        db2.execSQL("BEGIN DEFERRED TRANSACTION")
                        innerStatementCompiled.countDown()
                        thread2Result = it.readResult().first().values.first()
                        db2.execSQL("COMMIT")
                    }
                }
            }.apply {
                start()
            }

            val compiled = innerStatementCompiled.await(10, TimeUnit.SECONDS)

            assertThat(compiled).isTrue()

            backgroundThread.join(10.seconds.inWholeMilliseconds)

            db.execSQL("COMMIT")

            assertThat(thread2Result).isEqualTo("10")
        }
    }
}
