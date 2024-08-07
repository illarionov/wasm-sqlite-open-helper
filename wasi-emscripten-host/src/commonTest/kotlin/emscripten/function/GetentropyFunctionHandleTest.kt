/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import assertk.assertThat
import assertk.assertions.isNotZero
import assertk.assertions.isZero
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.EntropySource
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.test.assertions.hasBytesAt
import ru.pixnews.wasm.sqlite.open.helper.host.test.fixtures.TestEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.test.fixtures.TestMemory
import ru.pixnews.wasm.sqlite.test.utils.TestEnv
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class GetentropyFunctionHandleTest {
    private val host = TestEmbedderHost()
    private val memory = TestMemory()
    private val getentropyHandle = GetentropyFunctionHandle(host)

    @BeforeTest
    fun setup() {
        TestEnv.prepareTestEnvBeforeTest()
    }

    @AfterTest
    fun cleanup() {
        TestEnv.afterTest()
    }

    @Test
    fun getEntropy_success_case() {
        val testEntropySize = 32
        val testEntropy = ByteArray(testEntropySize) { (it + 3).toByte() }
        host.entropySource = EntropySource { size ->
            check(size == testEntropySize)
            testEntropy
        }
        val bufPtr: WasmPtr<Byte> = WasmPtr(128)

        val code = getentropyHandle.execute(memory, bufPtr, testEntropySize)

        assertThat(code).isZero()
        assertThat(memory).hasBytesAt(bufPtr, testEntropy)
    }

    @Test
    fun getEntropy_should_return_correct_code_on_fail() {
        host.entropySource = EntropySource { error("No entropy source") }
        val bufPtr: WasmPtr<Byte> = WasmPtr(128)
        val code = getentropyHandle.execute(memory, bufPtr, -32)
        assertThat(code).isNotZero()
    }
}
