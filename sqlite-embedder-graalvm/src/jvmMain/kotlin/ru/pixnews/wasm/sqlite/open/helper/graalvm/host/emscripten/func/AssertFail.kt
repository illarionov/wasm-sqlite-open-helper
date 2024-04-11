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
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.AssertionFailedException

internal class AssertFail(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
    functionName: String = "__assert_fail",
) : BaseWasmNode(language, module, host, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, wasmInstance: WasmInstance): Nothing {
        val args = frame.arguments
        assertFail(
            memory(frame),
            args.getArgAsWasmPtr(0),
            args.getArgAsWasmPtr(1),
            args.getArgAsInt(2),
            args.getArgAsWasmPtr(3),
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun assertFail(
        memory: WasmMemory,
        condition: WasmPtr<Byte>,
        filename: WasmPtr<Byte>,
        line: Int,
        func: WasmPtr<Byte>,
    ): Nothing {
        throw AssertionFailedException(
            condition = memory.readString(condition.addr, null),
            filename = memory.readString(filename.addr, null),
            line = line,
            func = memory.readString(func.addr, null),
        )
    }
}
