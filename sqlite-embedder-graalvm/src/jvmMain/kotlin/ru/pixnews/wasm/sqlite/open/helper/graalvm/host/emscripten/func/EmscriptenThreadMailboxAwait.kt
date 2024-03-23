/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func

import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.pthread.Pthread

internal class EmscriptenThreadMailboxAwait(
    language: WasmLanguage,
    module: WasmModule,
    functionName: String = "_emscripten_thread_mailbox_await",
    @Suppress("UnusedPrivateProperty")
    private val posixThreadRef: () -> Pthread,
    rootLogger: Logger,
) : BaseWasmNode(language, module, functionName) {
    private val logger = rootLogger.withTag(EmscriptenThreadMailboxAwait::class.qualifiedName!!)
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance) {
        val args = frame.arguments
        val threadPtr = args.getArgAsInt(0)
        logger.v { "_emscripten_thread_mailbox_await($threadPtr): skip, not implemented" }
    }
}
