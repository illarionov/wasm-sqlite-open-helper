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
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsLong
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsUint
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysMmanMapFlags
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysMmanProt
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import kotlin.io.path.isRegularFile

internal class MmapJs(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
    functionName: String = "__mmap_js",
) : BaseWasmNode(language, module, host, functionName) {
    @Suppress("MagicNumber")
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Int {
        val args = frame.arguments
        return mmapJs(
            args.getArgAsInt(0),
            SysMmanProt(args.getArgAsUint(1)),
            SysMmanMapFlags(args.getArgAsUint(2)),
            Fd(args.getArgAsInt(3)),
            args.getArgAsLong(4).toULong(),
            args.getArgAsWasmPtr(5),
            args.getArgAsWasmPtr(6),
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun mmapJs(
        len: Int,
        prot: SysMmanProt,
        flags: SysMmanMapFlags,
        fd: Fd,
        offset: ULong,
        pAllocated: WasmPtr<Int>,
        pAddr: WasmPtr<WasmPtr<Byte>>,
    ): Int {
        logger.v { "mmapJs($fd, $len, $prot, $flags, $fd, $offset, $pAllocated, $pAddr): Not implemented" }

        return try {
           val channel = host.fileSystem.getStreamByFd(fd)
           if (!channel.path.isRegularFile()) {
               throw SysException(Errno.NODEV, "${channel.path} is not a file")
           }
            return -Errno.INVAL.code // Not Supported
        } catch (e: SysException) {
            logger.v { "mmapJs($fd, $len, $prot, $flags, $fd, $offset, $pAllocated, $pAddr): Error ${e.errNo}" }
            -e.errNo.code
        }
    }
}
