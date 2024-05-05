/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory

import co.touchlab.kermit.Severity.Verbose
import org.junit.jupiter.api.Test
import ru.pixnews.wasm.sqlite.open.helper.tests.AbstractCommonFactoryTest

class ChicoryBaseFactoryTest : AbstractCommonFactoryTest<ChicorySqliteEmbedderConfig>(
    factoryCreator = ChicoryFactoryCreator,
    dbLoggerSeverity = Verbose,
) {
    @Test
    override fun `Factory initialization should work`() {
        super.`Factory initialization should work`()
    }
}
