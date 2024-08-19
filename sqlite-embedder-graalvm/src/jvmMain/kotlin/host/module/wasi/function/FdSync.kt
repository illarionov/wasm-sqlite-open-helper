/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("FunctionNaming")

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.wasi.function

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function.FdSyncSyscallFdatasyncFunctionHandle

internal fun FdSync(
    language: WasmLanguage,
    module: WasmModule,
    host: EmbedderHost,
): BaseWasmNode<FdSyncSyscallFdatasyncFunctionHandle> = FdSync(
    language,
    module,
    FdSyncSyscallFdatasyncFunctionHandle.fdSync(host),
)

internal fun SyscallFdatasync(
    language: WasmLanguage,
    module: WasmModule,
    host: EmbedderHost,
): BaseWasmNode<FdSyncSyscallFdatasyncFunctionHandle> = FdSync(
    language,
    module,
    FdSyncSyscallFdatasyncFunctionHandle.syscallFdatasync(host),
)

private class FdSync(
    language: WasmLanguage,
    module: WasmModule,
    handle: FdSyncSyscallFdatasyncFunctionHandle,
) : BaseWasmNode<FdSyncSyscallFdatasyncFunctionHandle>(language, module, handle) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, wasmInstance: WasmInstance): Int {
        return fdSync(frame.arguments.getArgAsInt(0))
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun fdSync(fd: Int): Int = handle.execute(Fd(fd)).code
}
