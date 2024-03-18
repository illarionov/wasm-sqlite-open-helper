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
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.asWasmPtr
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
    instance: WasmInstance,
    host: SqliteEmbedderHost,
    functionName: String = "fd_read",
): BaseWasmNode = FdRead(language, instance, host, CHANGE_POSITION, functionName)

internal fun fdPread(
    language: WasmLanguage,
    instance: WasmInstance,
    host: SqliteEmbedderHost,
    functionName: String = "fd_pread",
): BaseWasmNode = FdRead(language, instance, host, DO_NOT_CHANGE_POSITION, functionName)

private class FdRead(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: SqliteEmbedderHost,
    private val strategy: ReadWriteStrategy,
    functionName: String = "fd_read",
) : BaseWasmNode(language, instance, functionName) {
    private val logger: Logger = host.rootLogger.withTag(FdRead::class.qualifiedName!!)

    @Suppress("MagicNumber")
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return fdRead(
            Fd(args[0] as Int),
            args.asWasmPtr(1),
            args[2] as Int,
            args.asWasmPtr(3),
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName", "VARIABLE_HAS_PREFIX")
    private fun fdRead(
        fd: Fd,
        pIov: WasmPtr<Iovec>,
        iovCnt: Int,
        pNum: WasmPtr<Int>,
    ): Int {
        val ioVecs: IovecArray = readIovecs(memory, pIov, iovCnt)
        return try {
            val channel = host.fileSystem.getStreamByFd(fd)
            val readBytes = memory.readFromChannel(channel, strategy, ioVecs)
            memory.writeI32(pNum, readBytes.toInt())
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.i(e) { "read() error" }
            e.errNo
        }.code
    }
}
