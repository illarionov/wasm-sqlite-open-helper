/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("FunctionNaming")

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.preview1.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function.FdSyncSyscallFdatasyncFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function.FdSyncSyscallFdatasyncFunctionHandle.Companion.fdSync
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function.FdSyncSyscallFdatasyncFunctionHandle.Companion.syscallFdatasync
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal fun FdSync(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
    functionName: String = "fd_sync",
): BaseWasmNode = FdSync(language, module, host, functionName, fdSync(host))

internal fun SyscallFdatasync(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
    functionName: String = "__syscall_fdatasync",
): BaseWasmNode = FdSync(language, module, host, functionName, syscallFdatasync(host))

private class FdSync(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
    functionName: String = "fd_sync",
    private val handle: FdSyncSyscallFdatasyncFunctionHandle,
) : BaseWasmNode(language, module, host, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, wasmInstance: WasmInstance): Int {
        val args = frame.arguments
        return fdSync(args.getArgAsInt(0))
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun fdSync(fd: Int): Int = handle.execute(Fd(fd)).code
}
