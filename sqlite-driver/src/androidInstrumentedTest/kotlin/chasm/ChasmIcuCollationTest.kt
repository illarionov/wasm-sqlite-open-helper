/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.driver.chasm

import androidx.test.platform.app.InstrumentationRegistry
import at.released.wasm.sqlite.driver.WasmSQLiteDriver
import at.released.wasm.sqlite.driver.test.base.tests.AbstractIcuCollationTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class ChasmIcuCollationTest : AbstractIcuCollationTest<WasmSQLiteDriver<*>>(
    driverCreator = ChasmSqliteDriverFactory,
) {
    @JvmField
    @Rule
    val tempFolder = TemporaryFolder(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir)

    override fun fileInTempDir(databaseName: String): String = tempFolder.root.resolve(databaseName).path
}
