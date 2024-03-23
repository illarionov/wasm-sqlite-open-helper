/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper

import androidx.sqlite.db.SupportSQLiteDatabase
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import co.touchlab.kermit.Severity.Debug
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.pixnews.wasm.sqlite.open.helper.util.KermitLogger
import ru.pixnews.wasm.sqlite.open.helper.util.createWasmSQLiteOpenHelper
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

class WasmSQLiteOpenHelperFactoryMultiplatformTest {
    val logger = KermitLogger("RequerySQLiteOpenHelperFactoryTest")
    val dbLogger = KermitLogger(tag = "WSOH", minSeverity = Debug)

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `Factory from multiple threads should work`() {
        val helper = createWasmSQLiteOpenHelper(tempDir, dbLogger)
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
    fun `Factory from multiple threads with active transaction should work`() {
        val helper = createWasmSQLiteOpenHelper(tempDir, dbLogger)
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

            val compiled = innerStatementCompiled.await(10, SECONDS)

            assertThat(compiled).isTrue()

            backgroundThread.join(10.seconds.inWholeMilliseconds)

            db.setTransactionSuccessful()
            db.endTransaction()

            assertThat(thread2Result).isEqualTo("10")
        }
    }
}
