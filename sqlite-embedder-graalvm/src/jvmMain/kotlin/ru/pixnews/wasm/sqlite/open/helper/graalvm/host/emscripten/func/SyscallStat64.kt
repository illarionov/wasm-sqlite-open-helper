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
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.pack
import ru.pixnews.wasm.sqlite.open.helper.host.memory.write

internal fun syscallLstat64(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
    functionName: String = "__syscall_lstat64",
): BaseWasmNode = SyscallStat64(
    language = language,
    module = module,
    functionName = functionName,
    followSymlinks = false,
    host = host,
    rootLogger = host.rootLogger,
    filesystem = host.fileSystem,
)

internal fun syscallStat64(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
    functionName: String = "__syscall_stat64",
): BaseWasmNode = SyscallStat64(
    language = language,
    module = module,
    functionName = functionName,
    followSymlinks = true,
    host = host,
    rootLogger = host.rootLogger,
    filesystem = host.fileSystem,
)

private class SyscallStat64(
    language: WasmLanguage,
    module: WasmModule,
    rootLogger: Logger,
    functionName: String,
    host: SqliteEmbedderHost,
    private val followSymlinks: Boolean = false,
    private val filesystem: FileSystem,
) : BaseWasmNode(
    language = language,
    module = module,
    functionName = functionName,
    host = host,
) {
    private val logger: Logger = rootLogger.withTag(SyscallStat64::class.qualifiedName!!)
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Int {
        val args = frame.arguments
        return stat64(
            memory(frame),
            args.getArgAsWasmPtr(0),
            args.getArgAsWasmPtr(1),
        )
    }

    @TruffleBoundary
    private fun stat64(
        memory: WasmMemory,
        pathnamePtr: WasmPtr<Byte>,
        dst: WasmPtr<StructStat>,
    ): Int {
        var path = ""
        val hostMemory = memory.toHostMemory()
        try {
            path = hostMemory.readNullTerminatedString(pathnamePtr)
            val stat = filesystem.stat(
                path = path,
                followSymlinks = followSymlinks,
            ).also {
                logger.v { "$functionName($path): $it" }
            }.pack()
            hostMemory.write(dst, stat)
        } catch (e: SysException) {
            logger.v { "$functionName(`$path`): error ${e.errNo}" }
            return -e.errNo.code
        }

        return 0
    }
}
