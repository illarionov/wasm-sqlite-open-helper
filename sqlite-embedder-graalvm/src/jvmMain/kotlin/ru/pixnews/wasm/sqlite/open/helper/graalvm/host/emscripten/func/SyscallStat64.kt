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
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.asWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.Host
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.pack
import ru.pixnews.wasm.sqlite.open.helper.host.memory.write

internal fun syscallLstat64(
    language: WasmLanguage,
    instance: WasmInstance,
    host: Host,
    functionName: String = "__syscall_lstat64",
): BaseWasmNode = SyscallStat64(
    language = language,
    instance = instance,
    functionName = functionName,
    followSymlinks = false,
    filesystem = host.fileSystem,
)

internal fun syscallStat64(
    language: WasmLanguage,
    instance: WasmInstance,
    host: Host,
    functionName: String = "__syscall_stat64",
): BaseWasmNode = SyscallStat64(
    language = language,
    instance = instance,
    functionName = functionName,
    followSymlinks = true,
    filesystem = host.fileSystem,
)

private class SyscallStat64(
    language: WasmLanguage,
    instance: WasmInstance,
    functionName: String,
    private val followSymlinks: Boolean = false,
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.withTag(SyscallStat64::class.qualifiedName!!),
) : BaseWasmNode(
    language = language,
    instance = instance,
    functionName = functionName,
) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return stat64(
            args.asWasmPtr(0),
            args.asWasmPtr(1),
        )
    }

    @TruffleBoundary
    private fun stat64(
        pathnamePtr: WasmPtr<Byte>,
        dst: WasmPtr<StructStat>,
    ): Int {
        var path = ""
        try {
            path = memory.readNullTerminatedString(pathnamePtr)
            val stat = filesystem.stat(
                path = path,
                followSymlinks = followSymlinks,
            ).also {
                logger.v { "$functionName($path): $it" }
            }.pack()
            memory.write(dst, stat)
        } catch (e: SysException) {
            logger.v { "$functionName(`$path`): error ${e.errNo}" }
            return -e.errNo.code
        }

        return 0
    }
}
