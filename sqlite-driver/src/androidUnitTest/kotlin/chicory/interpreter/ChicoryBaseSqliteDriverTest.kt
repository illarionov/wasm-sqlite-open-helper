/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.driver.chicory.interpreter

import at.released.wasm.sqlite.driver.WasmSQLiteDriver
import at.released.wasm.sqlite.driver.chicory.ChicoryAndroidUnitTestSqliteDriverFactory
import at.released.wasm.sqlite.driver.test.base.tests.AbstractBasicSqliteDriverTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class ChicoryBaseSqliteDriverTest : AbstractBasicSqliteDriverTest<WasmSQLiteDriver<*>>(
    driverCreator = ChicoryAndroidUnitTestSqliteDriverFactory,
) {
    @JvmField
    @Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()

    override fun fileInTempDir(databaseName: String): String = tempFolder.root.resolve(databaseName).path
}
