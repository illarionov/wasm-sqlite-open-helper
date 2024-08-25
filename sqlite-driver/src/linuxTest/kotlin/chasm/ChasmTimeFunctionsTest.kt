/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.chasm

import ru.pixnews.wasm.sqlite.driver.WasmSQLiteDriver
import ru.pixnews.wasm.sqlite.driver.test.base.tests.AbstractTimeFunctionsTest
import ru.pixnews.wasm.sqlite.test.utils.TempFolder
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test

class ChasmTimeFunctionsTest : AbstractTimeFunctionsTest<WasmSQLiteDriver<*>>(
    driverCreator = ChasmSqliteDriverFactory,
) {
    private lateinit var tempDir: TempFolder

    @BeforeTest
    fun setupTempDir() {
        tempDir = TempFolder.create()
    }

    @AfterTest
    fun cleanupTempDir() {
        tempDir.delete()
    }

    override fun fileInTempDir(databaseName: String): String = tempDir.resolve(databaseName)

    @Test
    @Ignore // TODO
    override fun localtime_modifier_should_work() {
        super.localtime_modifier_should_work()
    }
}
