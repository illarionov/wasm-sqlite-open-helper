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
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.asWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.Host
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy.DO_NOT_CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.ext.FdWriteExt.readCiovecs
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CioVec
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal fun fdWrite(
    language: WasmLanguage,
    instance: WasmInstance,
    host: Host,
    functionName: String = "fd_write",
): BaseWasmNode = FdWrite(language, instance, host, CHANGE_POSITION, functionName)

internal fun fdPwrite(
    language: WasmLanguage,
    instance: WasmInstance,
    host: Host,
    functionName: String = "fd_pwrite",
): BaseWasmNode = FdWrite(language, instance, host, DO_NOT_CHANGE_POSITION, functionName)

private class FdWrite(
    language: WasmLanguage,
    instance: WasmInstance,
    private val host: Host,
    private val strategy: ReadWriteStrategy,
    functionName: String = "fd_write",
    private val logger: Logger = Logger.withTag(FdWrite::class.qualifiedName!!),
) : BaseWasmNode(language, instance, functionName) {
    @Suppress("MagicNumber")
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext): Int {
        val args = frame.arguments
        return fdWrite(
            Fd(args[0] as Int),
            args.asWasmPtr(1),
            args[2] as Int,
            args.asWasmPtr(3),
        )
    }

    @TruffleBoundary
    @Suppress("MemberNameEqualsClassName", "VARIABLE_HAS_PREFIX")
    private fun fdWrite(
        fd: Fd,
        pCiov: WasmPtr<CioVec>,
        cIovCnt: Int,
        pNum: WasmPtr<Int>,
    ): Int {
        val cioVecs: CiovecArray = readCiovecs(memory, pCiov, cIovCnt)
        return try {
            val channel = host.fileSystem.getStreamByFd(fd)
            val writtenBytes = memory.writeToChannel(channel, strategy, cioVecs)
            memory.writeI32(pNum, writtenBytes.toInt())
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.i(e) { "write() error" }
            e.errNo
        }.code
    }
}
