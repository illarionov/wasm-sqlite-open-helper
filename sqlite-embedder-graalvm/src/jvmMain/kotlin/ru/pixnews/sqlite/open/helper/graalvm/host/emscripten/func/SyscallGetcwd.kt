/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.graalvm.host.emscripten.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import ru.pixnews.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.sqlite.open.helper.graalvm.ext.asWasmPtr
import ru.pixnews.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.sqlite.open.helper.graalvm.host.Host
import ru.pixnews.sqlite.open.helper.host.memory.encodeToNullTerminatedByteArray
import ru.pixnews.sqlite.open.helper.host.memory.write
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.Errno
import java.util.logging.Logger

internal class SyscallGetcwd(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    functionName: String = "__syscall_getcwd",
    private val logger: Logger = Logger.getLogger(SyscallGetcwd::class.qualifiedName),
) : BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return syscallGetcwd(
            args.asWasmPtr(0),
            args[1] as Int,
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun syscallGetcwd(
        dst: WasmPtr<Byte>,
        size: Int,
    ): Int {
        logger.finest { "getCwd(dst: $dst size: $size)" }
        if (size == 0) {
            return -Errno.INVAL.code
        }

        val path = host.fileSystem.getCwd()
        val pathBytes: ByteArray = path.encodeToNullTerminatedByteArray()

        if (size < pathBytes.size) {
            return -Errno.RANGE.code
        }
        memory.write(dst, pathBytes)

        return pathBytes.size
    }
}
