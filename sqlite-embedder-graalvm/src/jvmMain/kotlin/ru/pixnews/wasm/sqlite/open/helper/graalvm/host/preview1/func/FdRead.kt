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
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy.DO_NOT_CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.ext.FdReadExt.readIovecs
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Iovec
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.IovecArray

internal fun fdRead(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
    functionName: String = "fd_read",
): BaseWasmNode = FdRead(language, module, host, CHANGE_POSITION, functionName)

internal fun fdPread(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
    functionName: String = "fd_pread",
): BaseWasmNode = FdRead(language, module, host, DO_NOT_CHANGE_POSITION, functionName)

private class FdRead(
    language: WasmLanguage,
    module: WasmModule,
    override val host: SqliteEmbedderHost,
    private val strategy: ReadWriteStrategy,
    functionName: String = "fd_read",
) : BaseWasmNode(language, module, host, functionName) {
    private val logger: Logger = host.rootLogger.withTag(FdRead::class.qualifiedName!!)

    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, wasmInstance: WasmInstance): Int {
        val args = frame.arguments
        return fdRead(
            memory(frame),
            Fd(args.getArgAsInt(0)),
            args.getArgAsWasmPtr(1),
            args.getArgAsInt(2),
            args.getArgAsWasmPtr(3),
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName", "VARIABLE_HAS_PREFIX")
    private fun fdRead(
        memory: WasmMemory,
        fd: Fd,
        pIov: WasmPtr<Iovec>,
        iovCnt: Int,
        pNum: WasmPtr<Int>,
    ): Int {
        val hostMemory = memory.toHostMemory()
        val ioVecs: IovecArray = readIovecs(hostMemory, pIov, iovCnt)
        return try {
            val channel = host.fileSystem.getStreamByFd(fd)
            val readBytes = hostMemory.readFromChannel(channel, strategy, ioVecs)
            hostMemory.writeI32(pNum, readBytes.toInt())
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.i(e) { "read() error" }
            e.errNo
        }.code
    }
}
