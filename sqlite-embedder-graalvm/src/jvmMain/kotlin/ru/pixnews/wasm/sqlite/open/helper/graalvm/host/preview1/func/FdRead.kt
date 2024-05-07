/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.preview1.func

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
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function.FdReadFdPreadFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Iovec

internal fun fdRead(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
): BaseWasmNode<FdReadFdPreadFunctionHandle> = FdRead(
    language,
    module,
    FdReadFdPreadFunctionHandle.fdRead(host),
)

internal fun fdPread(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
): BaseWasmNode<FdReadFdPreadFunctionHandle> = FdRead(
    language,
    module,
    FdReadFdPreadFunctionHandle.fdPread(host),
)

private class FdRead(
    language: WasmLanguage,
    module: WasmModule,
    handle: FdReadFdPreadFunctionHandle,
) : BaseWasmNode<FdReadFdPreadFunctionHandle>(language, module, handle) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, wasmInstance: WasmInstance): Int {
        val args = frame.arguments
        return fdRead(
            memory(frame),
            args.getArgAsInt(0),
            args.getArgAsWasmPtr(1),
            args.getArgAsInt(2),
            args.getArgAsWasmPtr(3),
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun fdRead(
        memory: WasmMemory,
        fd: Int,
        pIov: WasmPtr<Iovec>,
        iovCnt: Int,
        pNum: WasmPtr<Int>,
    ): Int = handle.execute(memory.toHostMemory(), Fd(fd), pIov, iovCnt, pNum).code
}
