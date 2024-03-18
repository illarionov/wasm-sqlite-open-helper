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
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.asWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.AssertionFailedException

internal class AssertFail(
    language: WasmLanguage,
    instance: WasmInstance,
    functionName: String = "__assert_fail",
) : BaseWasmNode(language, instance, functionName) {
    @Suppress("MagicNumber")
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Nothing {
        val args = frame.arguments
        assertFail(
            args.asWasmPtr(0),
            args.asWasmPtr(1),
            args[2] as Int,
            args.asWasmPtr(3),
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun assertFail(
        condition: WasmPtr<Byte>,
        filename: WasmPtr<Byte>,
        line: Int,
        func: WasmPtr<Byte>,
    ): Nothing {
        throw AssertionFailedException(
            condition = memory.readNullTerminatedString(condition),
            filename = memory.readNullTerminatedString(filename),
            line = line,
            func = memory.readNullTerminatedString(func),
        )
    }
}
