/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func

import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.Host
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class SyscallFtruncate64(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "__syscall_ftruncate64",
    private val logger: Logger = Logger.withTag(SyscallFtruncate64::class.qualifiedName!!),
) : BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return syscallFtruncate64(
            args[0] as Int,
            (args[1] as Long).toULong(),
        )
    }

    @CompilerDirectives.TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun syscallFtruncate64(
        fd: Int,
        length: ULong,
    ): Int = try {
        host.fileSystem.ftruncate(Fd(fd), length)
        Errno.SUCCESS.code
    } catch (e: SysException) {
        logger.v { "ftruncate64($fd, $length): Error ${e.errNo}" }
        -e.errNo.code
    }
}
