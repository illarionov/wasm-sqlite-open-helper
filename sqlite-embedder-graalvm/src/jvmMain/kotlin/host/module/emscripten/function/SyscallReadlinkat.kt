/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.SyscallReadlinkatFunctionHandle

internal class SyscallReadlinkat(
    language: WasmLanguage,
    module: WasmModule,
    host: EmbedderHost,
) : BaseWasmNode<SyscallReadlinkatFunctionHandle>(language, module, SyscallReadlinkatFunctionHandle(host)) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Any {
        val args = frame.arguments
        val memory = memory(frame)

        val sizeOrErrno = readlinkAt(
            memory,
            rawDirFd = args.getArgAsInt(0),
            pathnamePtr = args.getArgAsWasmPtr(1),
            buf = args.getArgAsWasmPtr(2),
            bufSize = args.getArgAsInt(3),
        )
        return sizeOrErrno
    }

    @TruffleBoundary
    private fun readlinkAt(
        memory: WasmMemory,
        rawDirFd: Int,
        pathnamePtr: WasmPtr<Byte>,
        buf: WasmPtr<Byte>,
        bufSize: Int,
    ): Int = handle.execute(memory.toHostMemory(), rawDirFd, pathnamePtr, buf, bufSize)
}
