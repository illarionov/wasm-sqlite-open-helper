/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.graalvm

import co.touchlab.kermit.Severity.Verbose
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ru.pixnews.wasm.sqlite.driver.WasmSQLiteDriver
import ru.pixnews.wasm.sqlite.driver.test.base.tests.AbstractMultithreadingTest
import ru.pixnews.wasm.sqlite.open.helper.graalvm.GraalvmRuntimeInstance

class GraalvmMultithreadingTest : AbstractMultithreadingTest<WasmSQLiteDriver<GraalvmRuntimeInstance>>(
    driverCreator = GraalvmSqliteDriverFactory(),
    dbLoggerSeverity = Verbose,
) {
    @JvmField
    @Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()

    override fun createThread(driver: WasmSQLiteDriver<GraalvmRuntimeInstance>, runnable: Runnable): Thread {
        return driver.runtime.managedThreadFactory.newThread(runnable)
    }

    override fun fileInTempDir(databaseName: String): String = tempFolder.root.resolve(databaseName).path
}
