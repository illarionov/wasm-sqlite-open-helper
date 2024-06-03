/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.chicory

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Ignore
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import ru.pixnews.wasm.sqlite.driver.base.AndroidUserDatabaseSuspendFactory
import ru.pixnews.wasm.sqlite.driver.test.base.tests.AbstractBasicRoomTest
import ru.pixnews.wasm.sqlite.open.helper.chicory.ChicorySqliteEmbedderConfig

@Ignore("TODO: fix Unable to lock file: 'test.db.lck'.")
class ChicoryBasicRoomTest : AbstractBasicRoomTest<ChicorySqliteEmbedderConfig>(
    driverCreator = ChicorySqliteDriverFactory,
    databaseFactory = AndroidUserDatabaseSuspendFactory,
) {
    @JvmField
    @Rule
    val tempFolder = TemporaryFolder(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir)

    override val tempDir: String get() = tempFolder.root.path

    override fun beforeSetup() = checkChicorySdk()
}