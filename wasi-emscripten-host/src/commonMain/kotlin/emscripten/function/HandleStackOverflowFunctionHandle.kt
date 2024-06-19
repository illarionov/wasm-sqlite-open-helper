/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.stack.EmscriptenStack

public class HandleStackOverflowFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.HANDLE_STACK_OVERFLOW, host) {
    public fun execute(
        stackBindings: EmscriptenStack,
        requestedSp: WasmPtr<Byte>,
    ): Nothing {
        val base = stackBindings.emscriptenStackBase
        val end = stackBindings.emscriptenStackEnd
        error("Stack overflow (Attempt to set SP to $requestedSp, with stack limits [$end — $base])")
    }
}
