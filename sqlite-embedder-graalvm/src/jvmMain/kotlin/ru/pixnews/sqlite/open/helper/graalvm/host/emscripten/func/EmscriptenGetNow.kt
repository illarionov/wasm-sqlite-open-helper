/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.graalvm.host.emscripten.func

import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.sqlite.open.helper.graalvm.host.Host

internal class EmscriptenGetNow(
    language: WasmLanguage,
    instance: WasmInstance,
    @Suppress("UnusedPrivateProperty") private val host: Host,
    functionName: String = "emscripten_get_now",
) : BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Double {
        return emscriptenGetNow()
    }

    @CompilerDirectives.TruffleBoundary
    @Suppress("MemberNameEqualsClassName", "MagicNumber")
    private fun emscriptenGetNow(): Double = System.nanoTime() / 1_000_000.0
}
