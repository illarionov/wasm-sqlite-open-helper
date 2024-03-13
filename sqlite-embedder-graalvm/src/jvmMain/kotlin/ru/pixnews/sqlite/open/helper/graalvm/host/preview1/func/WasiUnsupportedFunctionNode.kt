/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.graalvm.host.preview1.func

import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.predefined.WasmBuiltinRootNode
import org.graalvm.wasm.predefined.wasi.types.Errno

internal class WasiUnsupportedFunctionNode(
    language: WasmLanguage,
    instance: WasmInstance,
    private val name: String
) : WasmBuiltinRootNode(
    language, instance
) {
    override fun builtinNodeName(): String = name

    override fun executeWithContext(frame: VirtualFrame, context: WasmContext?): Any {
        return Errno.Nosys // error code for function not supported;
    }
}
