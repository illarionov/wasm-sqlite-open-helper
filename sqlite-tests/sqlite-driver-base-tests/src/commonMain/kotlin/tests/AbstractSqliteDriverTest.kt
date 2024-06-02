/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.test.base.tests

import androidx.sqlite.SQLiteDriver
import co.touchlab.kermit.Severity
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import ru.pixnews.wasm.sqlite.test.utils.KermitLogger
import kotlin.test.BeforeTest

public abstract class AbstractSqliteDriverTest<E : SqliteEmbedderConfig>(
    val driverFactory: TestSqliteDriverFactory,
    dbLoggerSeverity: Severity = Severity.Info,
) {
    protected open val logger: KermitLogger = KermitLogger(this::class.java.simpleName)
    protected open val dbLogger: KermitLogger = KermitLogger(tag = "WasmSQLiteDriver", minSeverity = dbLoggerSeverity)
    abstract val tempDir: String

    // Supposed to be called before all @BeforeTest - functions of descendants
    open fun beforeSetup() {}

    @BeforeTest
    fun preSetup() {
        beforeSetup()
    }

    public open fun createWasmSQLiteDriver(
        sqlite3Binary: WasmSqliteConfiguration = driverFactory.defaultSqliteBinary,
        tempDir: String = this.tempDir,
    ): SQLiteDriver = driverFactory.create(tempDir, dbLogger, sqlite3Binary)
}
