/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.graalvm.host.preview1.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.sqlite.open.helper.graalvm.ext.asWasmPtr
import ru.pixnews.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.sqlite.open.helper.graalvm.host.Host
import ru.pixnews.sqlite.open.helper.host.wasi.ext.WasiEnvironmentFunc

internal class EnvironSizesGet(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "environ_sizes_get",
) : BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return environSizesGet(
            args.asWasmPtr(0),
            args.asWasmPtr(1),
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun environSizesGet(
        environCountAddr: WasmPtr<Int>,
        environSizeAddr: WasmPtr<Int>,
    ): Int {
        return WasiEnvironmentFunc.environSizesGet(
            envProvider = host.systemEnvProvider,
            memory = memory,
            environCountAddr = environCountAddr,
            environSizeAddr = environSizeAddr,
        ).code
    }
}
