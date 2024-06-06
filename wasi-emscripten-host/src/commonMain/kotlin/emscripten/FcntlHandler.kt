/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructFlock
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class FcntlHandler(
    private val fileSystem: FileSystem<*>,
    rootLogger: Logger,
) {
    private val logger: Logger = rootLogger.withTag("FcntlHandler")
    private val handlers: Map<UInt, FcntlOperationHandler> = mapOf(
        Fcntl.F_SETLK to FcntlSetLockOperation(),
    )

    fun invoke(
        memory: Memory,
        fd: Fd,
        operation: UInt,
        thirdArg: Int?,
    ): Int = try {
        val handler = handlers[operation] ?: throw SysException(Errno.INVAL, "Fcntl operation $operation not supported")
        handler.invoke(memory, fd, thirdArg)
    } catch (e: SysException) {
        logger.v(e) { "invoke($fd, $operation, $thirdArg) failed: ${e.message}" }
        -e.errNo.code
    }

    internal fun interface FcntlOperationHandler {
        fun invoke(
            memory: Memory,
            fd: Fd,
            varArgs: Int?,
        ): Int
    }

    @Suppress("OBJECT_IS_PREFERRED")
    internal inner class FcntlSetLockOperation : FcntlOperationHandler {
        override fun invoke(
            memory: Memory,
            fd: Fd,
            varArgs: Int?,
        ): Int {
            val structStatPtr: WasmPtr<StructFlock> = memory.readPtr(WasmPtr(checkNotNull(varArgs)))
            val flockPacked = memory.readBytes(structStatPtr, StructFlock.STRUCT_FLOCK_SIZE)
            val flock = StructFlock.unpack(flockPacked)

            logger.v { "F_SETLK($fd, $flock)" }
            when (flock.l_type) {
                Fcntl.F_RDLCK, Fcntl.F_WRLCK -> fileSystem.addAdvisoryLock(fd, flock)
                Fcntl.F_UNLCK -> fileSystem.removeAdvisoryLock(fd, flock)
                else -> throw SysException(Errno.INVAL, "Unknown flock.l_type `${flock.l_type}`")
            }
            return Errno.SUCCESS.code
        }
    }
}
