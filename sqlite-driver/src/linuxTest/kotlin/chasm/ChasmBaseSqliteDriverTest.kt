/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.chasm

import ru.pixnews.wasm.sqlite.driver.WasmSQLiteDriver
import ru.pixnews.wasm.sqlite.driver.test.base.tests.AbstractBasicSqliteDriverTest
import ru.pixnews.wasm.sqlite.test.utils.TempFolder
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ChasmBaseSqliteDriverTest : AbstractBasicSqliteDriverTest<WasmSQLiteDriver<*>>(
    driverCreator = ChasmSqliteDriverFactory,
) {
    private lateinit var tempDir: TempFolder

    override fun fileInTempDir(databaseName: String): String = tempDir.resolve(databaseName)

    @BeforeTest
    fun setup() {
        tempDir = TempFolder.create()
    }

    @AfterTest
    fun cleanup() {
        tempDir.delete()
    }

    @Test
    override fun Driver_initialization_should_work() {
        super.Driver_initialization_should_work()
    }

    @Test
    override fun Driver_initialization_with_in_memory_database_should_work() {
        super.Driver_initialization_with_in_memory_database_should_work()
    }
}
