/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.graalvm.host

import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage

internal val notImplementedFunctionNodeFactory: NodeFactory = { language, instance, _, name ->
    NotImplementedBaseWasmNode(language, instance, name)
}

private class NotImplementedBaseWasmNode(
    language: WasmLanguage,
    instance: WasmInstance,
    private val name: String,
) : BaseWasmNode(language, instance, name) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Any {
        error("`$name`not implemented")
    }
}
