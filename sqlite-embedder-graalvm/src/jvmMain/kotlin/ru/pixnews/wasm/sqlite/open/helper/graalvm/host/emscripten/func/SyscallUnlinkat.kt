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
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.asWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno

internal class SyscallUnlinkat(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: SqliteEmbedderHost,
    functionName: String = "__syscall_unlinkat",
    @Suppress("UnusedPrivateProperty")
    private val logger: Logger = Logger.withTag(SyscallUnlinkat::class.qualifiedName!!),
) : BaseWasmNode(language, instance, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return syscallUnlinkat(
            args[0] as Int,
            args.asWasmPtr(1),
            (args[2] as Int).toUInt(),
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun syscallUnlinkat(
        dirfd: Int,
        pathnamePtr: WasmPtr<Byte>,
        flags: UInt,
    ): Int {
        val errNo = try {
            val path = memory.readNullTerminatedString(pathnamePtr)
            host.fileSystem.unlinkAt(dirfd, path, flags)
            Errno.SUCCESS
        } catch (e: SysException) {
            e.errNo
        }
        return -errNo.code
    }
}
