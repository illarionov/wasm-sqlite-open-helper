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
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.readNullTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.AssertionFailedException
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction

public class AssertFailFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.ASSERT_FAIL, host) {
    public fun execute(
        memory: Memory,
        condition: WasmPtr<Byte>,
        filename: WasmPtr<Byte>,
        line: Int,
        func: WasmPtr<Byte>,
    ): Nothing {
        throw AssertionFailedException(
            condition = memory.readNullTerminatedString(condition),
            filename = memory.readNullTerminatedString(filename),
            line = line,
            func = memory.readNullTerminatedString(func),
        )
    }
}
