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
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsUint
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class SyscallFchmod(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
    functionName: String = "__syscall_fchmod",
) : BaseWasmNode(language, module, host, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Int {
        val args = frame.arguments
        return syscallFchmod(
            args.getArgAsInt(0),
            args.getArgAsUint(1),
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun syscallFchmod(
        fd: Int,
        mode: UInt,
    ): Int {
        val fileMode = FileMode(mode)
        return try {
            host.fileSystem.chmod(Fd(fd), fileMode)
            Errno.SUCCESS.code
        } catch (e: SysException) {
            logger.v { "fchmod($fd, $fileMode): Error ${e.errNo}" }
            -e.errNo.code
        }
    }
}
