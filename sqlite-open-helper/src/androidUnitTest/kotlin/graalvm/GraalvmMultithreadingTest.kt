/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.graalvm

import at.released.wasm.sqlite.open.helper.test.base.tests.AbstractMultithreadingTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class GraalvmMultithreadingTest : AbstractMultithreadingTest<GraalvmSqliteEmbedderConfig>(
    factoryCreator = GraalvmFactoryCreator(),
) {
    @JvmField
    @Rule
    val tempFolder: TemporaryFolder = TemporaryFolder()

    override val tempDir: String get() = tempFolder.root.path
}
