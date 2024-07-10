/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm

import assertk.assertThat
import assertk.assertions.isEqualTo
import ru.pixnews.wasm.sqlite.binary.reader.WasmSourceReader
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import kotlin.test.Test

class GraalvmSqliteEmbedderTest {
    @Test
    fun wasmSourceReader_should_Be_possible_to_override() {
        val commonConfig = object : WasmSqliteCommonConfig {
            override val logger: Logger = Logger
        }
        val testReader = WasmSourceReader { emptyList() }

        val configBuilder: GraalvmSqliteEmbedderConfig.() -> Unit = {
            wasmSourceReader = testReader
        }

        val mergedConfig = GraalvmSqliteEmbedder.mergeConfig(commonConfig, configBuilder)

        assertThat(mergedConfig.wasmSourceReader).isEqualTo(testReader)
    }
}
