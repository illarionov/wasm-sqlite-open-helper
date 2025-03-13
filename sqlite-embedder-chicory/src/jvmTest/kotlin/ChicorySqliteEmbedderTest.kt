/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.chicory

import assertk.assertThat
import assertk.assertions.isEqualTo
import at.released.cassettes.playhead.AssetManager
import at.released.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import at.released.weh.common.api.Logger
import kotlin.test.Test

class ChicorySqliteEmbedderTest {
    @Test
    fun wasmSourceReader_should_Be_possible_to_override() {
        val commonConfig = object : WasmSqliteCommonConfig {
            override val logger: Logger = Logger
            override val wasmReader: AssetManager = AssetManager
        }
        val newReader = AssetManager { emptyList() }

        val configBuilder: ChicorySqliteEmbedderConfig.() -> Unit = {
            wasmSourceReader = newReader
        }

        val mergedConfig = ChicorySqliteEmbedder.mergeConfig(commonConfig, configBuilder)

        assertThat(mergedConfig.wasmSourceReader).isEqualTo(newReader)
    }
}
