/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.base

import androidx.sqlite.SQLiteDriver
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Severity.Info
import org.junit.jupiter.api.io.TempDir
import ru.pixnews.wasm.sqlite.driver.base.util.KermitLogger
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import java.io.File

abstract class AbstractSqliteDriverTest<E : SqliteEmbedderConfig>(
    private val driverCreator: TestSqliteDriverCreator<E>,
    dbLoggerSeverity: Severity = Info,
) {
    protected open val logger = KermitLogger(this::class.java.simpleName)
    protected open val dbLogger = KermitLogger(tag = "WasmSQLiteDriver", minSeverity = dbLoggerSeverity)

    @TempDir
    protected lateinit var tempDir: File

    open fun createWasmSQLiteDriver(
        sqlite3Binary: WasmSqliteConfiguration = driverCreator.defaultSqliteBinary,
    ): SQLiteDriver = driverCreator.create(tempDir, dbLogger, sqlite3Binary)
}
