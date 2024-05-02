/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host

import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost

internal val notImplementedFunctionNodeFactory: NodeFactory = { language, module, host, name ->
    NotImplementedBaseWasmNode(language, module, host, name)
}

private class NotImplementedBaseWasmNode(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
    private val name: String,
) : BaseWasmNode(language, module, host, name) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Any {
        error("`$name`not implemented")
    }
}
