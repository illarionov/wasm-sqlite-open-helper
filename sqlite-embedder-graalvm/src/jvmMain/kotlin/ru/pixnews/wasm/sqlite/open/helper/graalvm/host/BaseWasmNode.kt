/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host

import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory
import org.graalvm.wasm.nodes.WasmRootNode
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory.WasmHostMemoryImpl

internal open class BaseWasmNode(
    language: WasmLanguage,
    private val module: WasmModule,
    protected open val host: SqliteEmbedderHost,
    val functionName: String,
) : WasmRootNode(language, null, null) {
    override fun getName(): String = "wasm-function:$functionName"

    override fun module(): WasmModule = module

    fun WasmMemory.toHostMemory() = WasmHostMemoryImpl(this, this@BaseWasmNode, host.rootLogger)
}
