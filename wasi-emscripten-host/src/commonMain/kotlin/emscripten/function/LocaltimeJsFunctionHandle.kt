/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructTm
import ru.pixnews.wasm.sqlite.open.helper.host.include.pack

public class LocaltimeJsFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.LOCALTIME_JS, host) {
    public fun execute(
        memory: Memory,
        timeSeconds: Long,
        timePtr: WasmPtr<StructTm>,
    ) {
        val localTime = host.localTimeFormatter.format(timeSeconds)
        logger.v { "localtimeJs($timeSeconds): $localTime" }

        val bytes = localTime.pack()
        memory.write(timePtr, bytes)
    }
}
