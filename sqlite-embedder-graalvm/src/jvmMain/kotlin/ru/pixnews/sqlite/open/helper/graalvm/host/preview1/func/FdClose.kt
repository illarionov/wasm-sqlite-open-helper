/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.graalvm.host.preview1.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.sqlite.open.helper.graalvm.host.Host
import ru.pixnews.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.Fd
import java.util.logging.Level
import java.util.logging.Logger

internal class FdClose(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "fd_close",
    private val logger: Logger = Logger.getLogger(FdClose::class.qualifiedName),
) : BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return fdClose(Fd(args[0] as Int))
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun fdClose(
        fd: Fd,
    ): Int {
        return try {
            host.fileSystem.close(fd)
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.log(Level.INFO, e) { "fd_close() error: $e" }
            e.errNo
        }.code
    }
}
