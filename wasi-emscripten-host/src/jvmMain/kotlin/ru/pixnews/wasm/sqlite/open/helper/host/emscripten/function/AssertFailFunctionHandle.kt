/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readZeroTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.AssertionFailedException
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory

public class AssertFailFunctionHandle(
    host: SqliteEmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.ASSERT_FAIL, host) {
    public fun execute(
        memory: Memory,
        condition: WasmPtr<Byte>,
        filename: WasmPtr<Byte>,
        line: Int,
        func: WasmPtr<Byte>,
    ): Nothing {
        throw AssertionFailedException(
            condition = memory.readZeroTerminatedString(condition),
            filename = memory.readZeroTerminatedString(filename),
            line = line,
            func = memory.readZeroTerminatedString(func),
        )
    }
}
