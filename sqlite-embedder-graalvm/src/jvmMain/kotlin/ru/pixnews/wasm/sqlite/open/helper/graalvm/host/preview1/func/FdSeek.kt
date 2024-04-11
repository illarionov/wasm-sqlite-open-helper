/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.preview1.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsLong
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.position
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Whence

internal class FdSeek(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
    functionName: String = "fd_seek",
) : BaseWasmNode(language, module, host, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, wasmInstance: WasmInstance): Int {
        val args = frame.arguments
        return fdSeek(
            memory(frame),
            Fd(args.getArgAsInt(0)),
            args.getArgAsLong(1),
            args.getArgAsInt(2),
            args.getArgAsWasmPtr(3),
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName", "VARIABLE_HAS_PREFIX")
    private fun fdSeek(
        memory: WasmMemory,
        fd: Fd,
        offset: Long,
        whenceInt: Int,
        pNewOffset: WasmPtr<Long>,
    ): Int {
        val whence = Whence.fromIdOrNull(whenceInt) ?: return Errno.INVAL.code
        return try {
            val channel: FdChannel = host.fileSystem.getStreamByFd(fd)
            host.fileSystem.seek(channel, offset, whence)

            val newPosition = channel.position

            memory.store_i64(this, pNewOffset.addr.toLong(), newPosition)

            Errno.SUCCESS
        } catch (sysException: SysException) {
            logger.i(sysException) { "fdSeek() error" }
            sysException.errNo
        }.code
    }
}
