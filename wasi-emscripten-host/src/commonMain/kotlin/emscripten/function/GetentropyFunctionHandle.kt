/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.write
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
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
            val entropy = host.entropySource.invoke(size)
            check(entropy.size == size)
            memory.write(buffer, entropy)
            0
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            logger.e(e) { "getentropy() failed" }
            -1
        }
    }
}
