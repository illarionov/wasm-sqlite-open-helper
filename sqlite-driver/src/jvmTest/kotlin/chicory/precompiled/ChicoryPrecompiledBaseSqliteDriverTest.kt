/*
 * Copyright 2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.driver.chicory.precompiled

import at.released.wasm.sqlite.driver.WasmSQLiteDriver
import at.released.wasm.sqlite.driver.chicory.ChicoryPrecompiledSqliteDriverFactory
import at.released.wasm.sqlite.driver.test.base.tests.AbstractBasicSqliteDriverTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ChicoryPrecompiledBaseSqliteDriverTest : AbstractBasicSqliteDriverTest<WasmSQLiteDriver<*>>(
    driverCreator = ChicoryPrecompiledSqliteDriverFactory,
) {
    @JvmField
    @Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()

    override fun fileInTempDir(databaseName: String): String = tempFolder.root.resolve(databaseName).path

    @Test
    override fun Driver_initialization_should_work() {
        super.Driver_initialization_should_work()
    }

    @Test
    override fun Driver_initialization_with_in_memory_database_should_work() {
        super.Driver_initialization_with_in_memory_database_should_work()
    }
}
