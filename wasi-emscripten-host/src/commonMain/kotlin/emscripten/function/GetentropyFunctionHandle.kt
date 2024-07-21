/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import kotlinx.io.Buffer
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction

public class GetentropyFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.GETENTROPY, host) {
    public fun execute(
        memory: Memory,
        buffer: WasmPtr<Byte>,
        size: Int,
    ): Int {
        return try {
            val entropyBytes = host.entropySource.generateEntropy(size)
            check(entropyBytes.size == size)
            val entropySource = Buffer().apply { write(entropyBytes) }
            memory.write(
                fromSource = entropySource,
                toAddr = buffer,
                writeBytes = size,
            )
            0
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            logger.e(e) { "getentropy() failed" }
            -1
        }
    }
}
