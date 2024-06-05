/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.graalvm

import org.junit.Assume
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import ru.pixnews.wasm.sqlite.driver.base.AndroidUserDatabaseSuspendFactory
import ru.pixnews.wasm.sqlite.driver.test.base.tests.room.AbstractBasicMultithreadingRoomTest
import ru.pixnews.wasm.sqlite.open.helper.graalvm.GraalvmSqliteEmbedderConfig

class GraalvmBasicMultithreadingRoomTest : AbstractBasicMultithreadingRoomTest<GraalvmSqliteEmbedderConfig>(
    driverFactory = GraalvmSqliteDriverFactory(),
    databaseFactory = AndroidUserDatabaseSuspendFactory,
) {
    @JvmField
    @Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()

    override fun fileInTempDir(databaseName: String): String = tempFolder.root.resolve(databaseName).path

    @Ignore("TODO: fix")
    override fun Test_Room_Multithread() {
        Assume.assumeTrue("TODO: fix", false)
        // super.Test_Room_Multithread()
    }

    @Test
    override fun Test_In_Memory_Room_Multithread() {
        super.Test_In_Memory_Room_Multithread()
    }
}
