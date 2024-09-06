/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm

import assertk.assertThat
import assertk.assertions.isEqualTo
import at.released.weh.common.api.Logger
import ru.pixnews.wasm.sqlite.binary.reader.WasmSourceReader
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import kotlin.test.Test

class ChasmSqliteEmbedderTest {
    @Test
    fun wasmSourceReader_should_Be_possible_to_override() {
        val commonConfig = object : WasmSqliteCommonConfig {
            override val logger: Logger = Logger
            override val wasmReader: WasmSourceReader = WasmSourceReader
        }
        val newReader = WasmSourceReader { emptyList() }

        val configBuilder: ChasmSqliteEmbedderConfig.() -> Unit = {
            wasmSourceReader = newReader
        }

        val mergedConfig = ChasmSqliteEmbedder.mergeConfig(commonConfig, configBuilder)

        assertThat(mergedConfig.wasmSourceReader).isEqualTo(newReader)
    }
}
