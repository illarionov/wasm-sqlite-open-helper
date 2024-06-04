/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.graalvm

import co.touchlab.kermit.Severity.Verbose
import kotlinx.coroutines.debug.junit4.CoroutinesTimeout
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ru.pixnews.wasm.sqlite.driver.base.JvmDatabaseFactory
import ru.pixnews.wasm.sqlite.driver.test.base.tests.AbstractBasicRoomTest
import ru.pixnews.wasm.sqlite.open.helper.graalvm.GraalvmSqliteEmbedderConfig

class GraalvmBasicRoomTest : AbstractBasicRoomTest<GraalvmSqliteEmbedderConfig>(
    driverCreator = GraalvmSqliteDriverFactory(),
    databaseFactory = JvmDatabaseFactory,
    dbLoggerSeverity = Verbose,
) {
    @get:Rule
    public val timeout = CoroutinesTimeout.seconds(600)

    @JvmField
    @Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()

    override val tempDir: String get() = tempFolder.root.path
}
