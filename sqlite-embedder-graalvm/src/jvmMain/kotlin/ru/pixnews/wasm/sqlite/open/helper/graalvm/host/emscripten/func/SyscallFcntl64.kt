/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func

import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.FcntlHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class SyscallFcntl64(
    language: WasmLanguage,
    module: WasmModule,
    private val host: SqliteEmbedderHost,
    functionName: String = "__syscall_fcntl64",
) : BaseWasmNode(language, module, functionName) {
    private val logger: Logger = host.rootLogger.withTag(SyscallFcntl64::class.qualifiedName!!)
    private val fcntlHandler = FcntlHandler(host.fileSystem, host.rootLogger)

    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, wasmInstance: WasmInstance): Int {
        val args = frame.arguments
        return syscallFcntl64(
            memory(frame),
            Fd(args.getArgAsInt(0)),
            args.getArgAsInt(1),
            args.getArgAsInt(2),
        )
    }

    @Suppress("MemberNameEqualsClassName")
    private fun syscallFcntl64(
        memory: WasmMemory,
        fd: Fd,
        cmd: Int,
        thirdArg: Int,
    ): Int {
        return try {
            fcntlHandler.invoke(memory.toHostMemory(), fd, cmd.toUInt(), thirdArg)
        } catch (e: SysException) {
            logger.v(e) { "__syscall_fcntl64() failed: ${e.message}" }
            e.errNo.code
        }
    }
}
