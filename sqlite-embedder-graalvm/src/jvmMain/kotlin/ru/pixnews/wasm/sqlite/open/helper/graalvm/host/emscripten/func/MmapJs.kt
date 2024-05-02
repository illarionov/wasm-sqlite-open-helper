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
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsLong
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsUint
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.MmapJsFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysMmanMapFlags
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysMmanProt
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class MmapJs(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
    functionName: String = "__mmap_js",
) : BaseWasmNode(language, module, host, functionName) {
    private val handle = MmapJsFunctionHandle(host)

    @Suppress("MagicNumber")
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Int {
        val args = frame.arguments
        return mmapJs(
            args.getArgAsInt(0),
            args.getArgAsUint(1),
            args.getArgAsUint(2),
            args.getArgAsInt(3),
            args.getArgAsLong(4).toULong(),
            args.getArgAsWasmPtr(5),
            args.getArgAsWasmPtr(6),
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun mmapJs(
        len: Int,
        prot: UInt,
        flags: UInt,
        fd: Int,
        offset: ULong,
        pAllocated: WasmPtr<Int>,
        pAddr: WasmPtr<WasmPtr<Byte>>,
    ): Int = handle.execute(len, SysMmanProt(prot), SysMmanMapFlags(flags), Fd(fd), offset, pAllocated, pAddr)
}
