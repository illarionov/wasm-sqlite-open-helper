/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.chasm

import ru.pixnews.wasm.sqlite.driver.WasmSQLiteDriver
import ru.pixnews.wasm.sqlite.driver.base.LinuxDatabaseFactory
import ru.pixnews.wasm.sqlite.driver.test.base.tests.room.AbstractBasicRoomTest
import ru.pixnews.wasm.sqlite.test.utils.TempFolder
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

class ChasmBasicRoomTest : AbstractBasicRoomTest<WasmSQLiteDriver<*>>(
    driverFactory = ChasmSqliteDriverFactory,
    databaseFactory = LinuxDatabaseFactory(),
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
}
