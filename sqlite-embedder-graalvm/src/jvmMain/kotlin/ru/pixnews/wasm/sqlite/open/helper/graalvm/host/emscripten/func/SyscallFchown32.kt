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
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class SyscallFchown32(
    language: WasmLanguage,
    module: WasmModule,
    override val host: SqliteEmbedderHost,
    functionName: String = "__syscall_fchown32",
) : BaseWasmNode(language, module, host, functionName) {
    private val logger: Logger = host.rootLogger.withTag(SyscallFchown32::class.qualifiedName!!)

    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Int {
        val args = frame.arguments
        return syscallFchown32(
            args.getArgAsInt(0),
            args.getArgAsInt(1),
            args.getArgAsInt(2),
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun syscallFchown32(
        fd: Int,
        owner: Int,
        group: Int,
    ): Int = try {
        host.fileSystem.chown(Fd(fd), owner, group)
        Errno.SUCCESS.code
    } catch (e: SysException) {
        logger.v { "chown($fd, $owner, $group): Error ${e.errNo}" }
        -e.errNo.code
    }
}
