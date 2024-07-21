/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.chicory.interpreter

import androidx.sqlite.SQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ru.pixnews.wasm.sqlite.driver.base.AndroidUserDatabaseSuspendFactory
import ru.pixnews.wasm.sqlite.driver.chicory.ChicoryAndroidInstrumentedSqliteDriverFactory
import ru.pixnews.wasm.sqlite.driver.chicory.checkChicorySdk
import ru.pixnews.wasm.sqlite.driver.test.base.tests.room.AbstractBasicRoomTest

class ChicoryBasicRoomTest : AbstractBasicRoomTest<SQLiteDriver>(
    driverFactory = ChicoryAndroidInstrumentedSqliteDriverFactory,
    databaseFactory = AndroidUserDatabaseSuspendFactory,
) {
    @JvmField
    @Rule
    val tempFolder = TemporaryFolder(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir)

    override fun fileInTempDir(databaseName: String): String = tempFolder.root.resolve(databaseName).path

    override fun beforeSetup() = checkChicorySdk()
}
