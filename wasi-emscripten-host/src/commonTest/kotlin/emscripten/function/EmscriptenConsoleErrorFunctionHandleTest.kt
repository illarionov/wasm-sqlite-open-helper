/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.writeNullTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.host.test.fixtures.TestEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.test.fixtures.TestMemory
import ru.pixnews.wasm.sqlite.test.utils.TestLogger
import kotlin.test.Test

class EmscriptenConsoleErrorFunctionHandleTest {
    private val host = TestEmbedderHost()
    private val memory = TestMemory()

    @Test
    fun consoleErrorLog_success_case() {
        var errorLogged = false
        var loggedThrowable: Throwable? = null
        var loggedMessage: String? = null
        host.rootLogger = object : TestLogger() {
            override fun e(throwable: Throwable?, message: () -> String) {
                errorLogged = true
                loggedThrowable = throwable
                loggedMessage = message()
            }
        }
        val handle = EmscriptenConsoleErrorFunctionHandle(host)

        val testMessage = "Test message"
        val messagePtr: WasmPtr<Byte> = WasmPtr(128)
        memory.writeNullTerminatedString(messagePtr, testMessage)

        handle.execute(memory, messagePtr)

        assertThat(errorLogged).isTrue()
        assertThat(loggedThrowable).isNull()
        assertThat(loggedMessage).isEqualTo(testMessage)
    }
}
