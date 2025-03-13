/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.test.base.tests

import androidx.sqlite.SQLiteDriver
import at.released.wasm.sqlite.binary.base.WasmSqliteConfiguration
import at.released.weh.common.api.Logger
import at.released.weh.test.logger.TestLogger
import co.touchlab.kermit.Severity
import ru.pixnews.wasm.sqlite.test.utils.TestEnv
import kotlin.test.BeforeTest

public abstract class AbstractSqliteDriverTest<S>(
    val driverFactory: TestSqliteDriverFactory<S>,
    dbLoggerSeverity: Severity = Severity.Info,
) where S : SQLiteDriver, S : AutoCloseable {
    protected open val logger: Logger = TestLogger("AbstractSqliteDriverTest")
    protected open val dbLogger: Logger = TestLogger(tag = "WasmSQLiteDriver", minSeverity = dbLoggerSeverity)

    abstract fun fileInTempDir(databaseName: String): String

    // Supposed to be called before all @BeforeTest - functions of descendants
    open fun beforeSetup() {}

    @BeforeTest
    fun preSetup() {
        TestEnv.prepareTestEnvBeforeTest()
        beforeSetup()
    }

    public fun createWasmSQLiteDriver(
        sqlite3Binary: WasmSqliteConfiguration = driverFactory.defaultSqliteBinary,
    ): S = driverFactory.create(dbLogger, sqlite3Binary)
}
