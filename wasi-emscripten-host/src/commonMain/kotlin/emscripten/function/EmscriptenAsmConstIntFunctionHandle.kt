/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction

public class EmscriptenAsmConstIntFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.EMSCRIPTEN_ASM_CONST_INT, host) {
    public fun execute(
        emAsmAddr: WasmPtr<Byte>,
        sigPtr: WasmPtr<Byte>,
        argbuf: WasmPtr<Byte>,
    ): Int {
        logger.i { "emscripten_asm_const_int($emAsmAddr, $sigPtr, $argbuf): Not implemented" }
        return 0
    }
}
