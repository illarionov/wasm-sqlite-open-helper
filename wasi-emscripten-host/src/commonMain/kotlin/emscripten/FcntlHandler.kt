/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten

import kotlinx.io.buffered
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.ReadOnlyMemory
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.readPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.sourceWithMaxSize
import ru.pixnews.wasm.sqlite.open.helper.host.ext.negativeErrnoCode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.AddAdvisoryLockFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.RemoveAdvisoryLockFd
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructFlock
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructFlock.Companion.STRUCT_FLOCK_SIZE
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.INVAL
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal class FcntlHandler(
    private val fileSystem: FileSystem,
    rootLogger: Logger,
) {
    private val logger: Logger = rootLogger.withTag("FcntlHandler")
    private val handlers: Map<UInt, FcntlOperationHandler> = mapOf(
        Fcntl.F_SETLK to FcntlSetLockOperation(),
    )

    fun invoke(
        memory: ReadOnlyMemory,
        fd: Fd,
        operation: UInt,
        thirdArg: Int?,
    ): Int {
        val handler = handlers[operation] ?: return -INVAL.code
        return handler.invoke(memory, fd, thirdArg)
    }

    internal fun interface FcntlOperationHandler {
        fun invoke(
            memory: ReadOnlyMemory,
            fd: Fd,
            varArgs: Int?,
        ): Int
    }

    @Suppress("OBJECT_IS_PREFERRED")
    internal inner class FcntlSetLockOperation : FcntlOperationHandler {
        override fun invoke(
            memory: ReadOnlyMemory,
            fd: Fd,
            varArgs: Int?,
        ): Int {
            val structStatPtr: WasmPtr<StructFlock> = memory.readPtr(WasmPtr(checkNotNull(varArgs)))
            val flock = memory.sourceWithMaxSize(structStatPtr, STRUCT_FLOCK_SIZE).buffered().use {
                StructFlock.unpack(it)
            }

            logger.v { "F_SETLK($fd, $flock)" }
            return when (flock.l_type) {
                Fcntl.F_RDLCK, Fcntl.F_WRLCK -> fileSystem.execute(
                    AddAdvisoryLockFd,
                    AddAdvisoryLockFd(fd, flock),
                ).negativeErrnoCode()

                Fcntl.F_UNLCK -> fileSystem.execute(
                    RemoveAdvisoryLockFd,
                    RemoveAdvisoryLockFd(fd, flock),
                ).negativeErrnoCode()

                else -> -INVAL.code
            }
        }
    }
}
