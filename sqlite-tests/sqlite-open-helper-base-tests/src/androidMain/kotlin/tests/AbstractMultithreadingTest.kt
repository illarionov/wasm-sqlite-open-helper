/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.test.base.tests

import androidx.sqlite.db.SupportSQLiteDatabase
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Severity.Info
import org.junit.jupiter.api.Test
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import ru.pixnews.wasm.sqlite.open.helper.test.base.AbstractOpenHelperFactoryTest
import ru.pixnews.wasm.sqlite.open.helper.test.base.TestOpenHelperFactoryCreator
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

abstract class AbstractMultithreadingTest<E : SqliteEmbedderConfig>(
    factoryCreator: TestOpenHelperFactoryCreator,
    dbLoggerSeverity: Severity = Info,
) : AbstractOpenHelperFactoryTest<E>(
    factoryCreator = factoryCreator,
    dbLoggerSeverity = dbLoggerSeverity,
) {
    @Test
    fun Factory_from_multiple_threads_should_work() {
        val helper = createWasmSQLiteOpenHelper()
        helper.writableDatabase.use { db: SupportSQLiteDatabase ->
            logger.i { "db: $db; version: ${db.version}" }

            db.execSQL("CREATE TABLE t1(x, y)")
            db.execSQL("INSERT INTO t1 VALUES (1, 2), (3, 4)")

            var thread2Result: TimedValue<String?>? = null

            Thread {
                thread2Result = measureTimedValue {
                    db.compileStatement("SELECT sum(x+y) FROM t1").use {
                        it.simpleQueryForString()
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
    @Suppress("MagicNumber")
    fun Factory_from_multiple_threads_with_active_transaction_should_work() {
        val helper = createWasmSQLiteOpenHelper()
        helper.writableDatabase.use { db: SupportSQLiteDatabase ->
            logger.i { "db: $db; version: ${db.version}" }

            db.execSQL("CREATE TABLE t1(x, y)")
            db.execSQL("INSERT INTO t1 VALUES (1, 2), (3, 4)")
            db.enableWriteAheadLogging()
            db.beginTransactionNonExclusive()
            db.execSQL("INSERT INTO t1 VALUES (5, 6)")

            val innerStatementCompiled = CountDownLatch(1)
            var thread2Result: String? = null

            val backgroundThread = Thread {
                db.compileStatement("SELECT sum(x+y) FROM t1").use {
                    innerStatementCompiled.countDown()
                    thread2Result = it.simpleQueryForString()
                }
            }.apply {
                start()
            }

            val compiled = innerStatementCompiled.await(10, TimeUnit.SECONDS)

            assertThat(compiled).isTrue()

            backgroundThread.join(10.seconds.inWholeMilliseconds)

            db.setTransactionSuccessful()
            db.endTransaction()

            assertThat(thread2Result).isEqualTo("10")
        }
    }
}
