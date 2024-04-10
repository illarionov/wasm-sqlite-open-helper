/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper

import androidx.sqlite.db.SupportSQLiteDatabase
import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEqualTo
import co.touchlab.kermit.Severity.Info
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.pixnews.wasm.sqlite.open.helper.util.GraalvmEngine.WASM_GRAALVM_ENGINE
import ru.pixnews.wasm.sqlite.open.helper.util.KermitLogger
import ru.pixnews.wasm.sqlite.open.helper.util.createWasmSQLiteOpenHelper
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import kotlin.time.Duration.Companion.minutes

class WasmSQLiteOpenHelperTimeFunctionsTest {
    val dbLogger = KermitLogger(tag = "WSOH", minSeverity = Info)
    lateinit var database: SupportSQLiteDatabase

    @BeforeEach
    fun setup() {
        val helper = createWasmSQLiteOpenHelper(
            File("nonexistent"),
            dbLogger,
            dbName = null,
            wasmGraalvmEngine = WASM_GRAALVM_ENGINE,
        )
        database = helper.writableDatabase
    }

    @AfterEach
    fun destroy() {
        database.close()
    }

    @Test
    fun `date() should work`() {
        val dateString = database.compileStatement("""SELECT date()""").use {
            it.simpleQueryForString()
        }
        assertThat(dateString).isEqualTo(
            SimpleDateFormat("YYYY-MM-dd").format(Date()),
        )
    }

    @Test
    fun `unixepoch() should work`() {
        val timestampNow = Instant.now().epochSecond
        val unixEpoch = database.compileStatement("""SELECT unixepoch()""").use {
            it.simpleQueryForLong()
        }
        assertThat(unixEpoch).isBetween(timestampNow, timestampNow + 10.minutes.inWholeSeconds)
    }
}
