/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import kotlinx.io.buffered
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.ReadOnlyMemory
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.readPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.sourceWithMaxSize
import ru.pixnews.wasm.sqlite.open.helper.host.ext.negativeErrnoCode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno.INVAL
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Whence
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.lock.AddAdvisoryLockFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.lock.AdvisoryLockError.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.lock.Advisorylock
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.lock.AdvisorylockLockType
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.lock.RemoveAdvisoryLockFd
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructFlock
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructFlock.Companion.STRUCT_FLOCK_SIZE

internal class FcntlHandler(
    private val fileSystem: FileSystem,
) {
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
            val advisoryLock = flock.toAdvisoryLock().getOrElse {
                return -it.errno.code
            }
            return when (flock.l_type) {
                Fcntl.F_RDLCK, Fcntl.F_WRLCK -> fileSystem.execute(
                    AddAdvisoryLockFd,
                    AddAdvisoryLockFd(fd, advisoryLock),
                ).negativeErrnoCode()

                Fcntl.F_UNLCK -> fileSystem.execute(
                    RemoveAdvisoryLockFd,
                    RemoveAdvisoryLockFd(fd, advisoryLock),
                ).negativeErrnoCode()

                else -> -INVAL.code
            }
        }
    }

    private companion object {
        fun StructFlock.toAdvisoryLock(): Either<InvalidArgument, Advisorylock> {
            val type = when (this.l_type) {
                Fcntl.F_RDLCK -> AdvisorylockLockType.READ
                Fcntl.F_WRLCK -> AdvisorylockLockType.WRITE
                Fcntl.F_UNLCK -> AdvisorylockLockType.WRITE
                else -> return InvalidArgument("Incorrect l_type").left()
            }
            val whence = Whence.fromIdOrNull(this.l_whence.toInt())
                ?: return InvalidArgument("Incorrect whence `${this.l_whence}`").left()

            return Advisorylock(
                type = type,
                whence = whence,
                start = this.l_start.toLong(),
                length = this.l_len.toLong(),
                pid = this.l_pid,
            ).right()
        }
    }
}
