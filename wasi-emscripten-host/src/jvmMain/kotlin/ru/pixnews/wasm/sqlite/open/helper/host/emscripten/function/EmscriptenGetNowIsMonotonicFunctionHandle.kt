/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction

public class EmscriptenGetNowIsMonotonicFunctionHandle(
    host: SqliteEmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.EMSCRIPTEN_GET_NOW_IS_MONOTONIC, host) {
    @Suppress("FunctionOnlyReturningConstant")
    public fun execute(): Int = 1
}
