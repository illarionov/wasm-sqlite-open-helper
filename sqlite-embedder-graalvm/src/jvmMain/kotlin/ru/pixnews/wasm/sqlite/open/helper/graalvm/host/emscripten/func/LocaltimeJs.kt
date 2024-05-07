/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsLong
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.LocaltimeJsFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructTm

internal class LocaltimeJs(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
) : BaseWasmNode<LocaltimeJsFunctionHandle>(language, module, LocaltimeJsFunctionHandle(host)) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance) {
        val args = frame.arguments
        localtimeJs(
            memory(frame),
            args.getArgAsLong(0),
            args.getArgAsWasmPtr(1),
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun localtimeJs(
        memory: WasmMemory,
        time: Long,
        timePtr: WasmPtr<StructTm>,
    ) = handle.execute(memory.toHostMemory(), time, timePtr)
}
