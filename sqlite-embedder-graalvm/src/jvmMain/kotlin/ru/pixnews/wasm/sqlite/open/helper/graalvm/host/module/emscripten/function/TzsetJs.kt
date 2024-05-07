/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function

import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.TzsetJsFunctionHandle

internal class TzsetJs(
    language: WasmLanguage,
    module: WasmModule,
    host: EmbedderHost,
) : BaseWasmNode<TzsetJsFunctionHandle>(language, module, TzsetJsFunctionHandle(host)) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance) {
        val args = frame.arguments
        tzsetJs(
            memory(frame),
            args.getArgAsWasmPtr(0),
            args.getArgAsWasmPtr(1),
            args.getArgAsWasmPtr(2),
            args.getArgAsWasmPtr(3),
        )
    }

    @CompilerDirectives.TruffleBoundary
    @Suppress("MemberNameEqualsClassName", "MagicNumber")
    private fun tzsetJs(
        memory: WasmMemory,
        timezone: WasmPtr<Int>,
        daylight: WasmPtr<Int>,
        stdName: WasmPtr<Byte>,
        dstName: WasmPtr<Byte>,
    ) = handle.execute(memory.toHostMemory(), timezone, daylight, stdName, dstName)
}
