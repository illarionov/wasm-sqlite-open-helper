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
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class FdSync(
    language: WasmLanguage,
    module: WasmModule,
    private val host: SqliteEmbedderHost,
    functionName: String = "fd_sync",
) : BaseWasmNode(language, module, functionName) {
    private val logger: Logger = host.rootLogger.withTag(FdSync::class.qualifiedName!!)

    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, wasmInstance: WasmInstance): Int {
        val args = frame.arguments
        return fdSync(Fd(args.getArgAsInt(0)))
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun fdSync(
        fd: Fd,
    ): Int {
        return try {
            host.fileSystem.sync(fd, metadata = true)
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.i(e) { "sync() error" }
            e.errNo
        }.code
    }
}
