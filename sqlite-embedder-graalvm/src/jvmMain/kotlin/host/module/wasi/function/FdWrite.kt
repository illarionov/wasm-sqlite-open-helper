/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.wasi.function

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory.GraalOutputStreamWasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function.FdWriteFdPWriteFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CioVec

internal fun fdWrite(
    language: WasmLanguage,
    module: WasmModule,
    host: EmbedderHost,
): BaseWasmNode<FdWriteFdPWriteFunctionHandle> = FdWrite(language, module, FdWriteFdPWriteFunctionHandle.fdWrite(host))

internal fun fdPwrite(
    language: WasmLanguage,
    module: WasmModule,
    host: EmbedderHost,
): BaseWasmNode<FdWriteFdPWriteFunctionHandle> = FdWrite(language, module, FdWriteFdPWriteFunctionHandle.fdPwrite(host))

private class FdWrite(
    language: WasmLanguage,
    module: WasmModule,
    handle: FdWriteFdPWriteFunctionHandle,
) : BaseWasmNode<FdWriteFdPWriteFunctionHandle>(language, module, handle) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Any {
        val args = frame.arguments
        return fdWrite(
            memory(frame),
            args.getArgAsInt(0),
            args.getArgAsWasmPtr(1),
            args.getArgAsInt(2),
            args.getArgAsWasmPtr(3),
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun fdWrite(
        memory: WasmMemory,
        fd: Int,
        pCiov: WasmPtr<CioVec>,
        cIovCnt: Int,
        pNum: WasmPtr<Int>,
    ): Int {
        val hostMemory = memory.toHostMemory()
        val wasiMemoryWriter: WasiMemoryWriter = GraalOutputStreamWasiMemoryWriter(
            hostMemory,
            handle.host.fileSystem,
            handle.host.rootLogger,
        )
        return handle.execute(hostMemory, wasiMemoryWriter, Fd(fd), pCiov, cIovCnt, pNum).code
    }
}
