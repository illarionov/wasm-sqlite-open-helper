/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.encodeToNullTerminatedByteArray
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.write
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction

public class TzsetJsFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.TZSET_JS, host) {
    public fun execute(
        memory: Memory,
        timezone: WasmPtr<Int>,
        daylight: WasmPtr<Int>,
        stdName: WasmPtr<Byte>,
        dstName: WasmPtr<Byte>,
    ) {
        val tzInfo = host.timeZoneInfo()
        logger.v { "tzsetJs() TZ info: $tzInfo" }
        memory.writeI32(timezone, tzInfo.timeZone.toInt())
        memory.writeI32(daylight, tzInfo.daylight)

        val nameBytes = tzInfo.stdName.encodeToNullTerminatedByteArray(TZ_NAME_MAX_SIZE)
        memory.write(stdName, nameBytes)

        val dstNameBytes = tzInfo.dstName.encodeToNullTerminatedByteArray(TZ_NAME_MAX_SIZE)
        memory.write(dstName, dstNameBytes)
    }

    private companion object {
        private const val TZ_NAME_MAX_SIZE = 7
    }
}
