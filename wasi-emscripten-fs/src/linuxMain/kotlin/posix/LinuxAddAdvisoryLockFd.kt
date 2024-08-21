/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import platform.posix.EACCES
import platform.posix.EAGAIN
import platform.posix.EBADF
import platform.posix.EINTR
import platform.posix.EINVAL
import platform.posix.ENOLCK
import platform.posix.F_RDLCK
import platform.posix.F_SETLK
import platform.posix.F_WRLCK
import platform.posix.errno
import platform.posix.fcntl
import platform.posix.flock
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.AdvisoryLockError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Again
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Interrupted
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NoLock
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.lock.AddAdvisoryLockFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.lock.Advisorylock
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.lock.AdvisorylockLockType
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.lock.AdvisorylockLockType.READ
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.lock.AdvisorylockLockType.WRITE
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base.PosixOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.ext.toPosixWhence

internal object LinuxAddAdvisoryLockFd : PosixOperationHandler<AddAdvisoryLockFd, AdvisoryLockError, Unit> {
    override fun invoke(input: AddAdvisoryLockFd): Either<AdvisoryLockError, Unit> = memScoped {
        val structFlockInstance: flock = alloc<flock> {
            setFromAdvisoryLock(input.flock)
        }
        val exitCode = fcntl(
            input.fd.fd,
            F_SETLK,
            structFlockInstance,
        )
        return if (exitCode == 0) {
            Unit.right()
        } else {
            errno.errnoToAdvisoryLockError(input.fd, input.flock).left()
        }
    }

    internal fun flock.setFromAdvisoryLock(lock: Advisorylock) {
        l_type = lock.type.toFlockType()
        l_whence = lock.whence.toPosixWhence().toShort()
        l_start = lock.start
        l_len = lock.length
        l_pid = 0
    }

    internal fun AdvisorylockLockType.toFlockType(): Short = when (this) {
        READ -> F_RDLCK
        WRITE -> F_WRLCK
    }.toShort()

    internal fun Int.errnoToAdvisoryLockError(fd: Fd, lock: Advisorylock): AdvisoryLockError = when (this) {
        EACCES, EAGAIN -> Again("Can not lock `$fd - $lock`, operation prohibited`")
        EBADF -> BadFileDescriptor("Bad file descriptor $fd")
        EINTR -> Interrupted("Locking $fd interrupted by signal")
        EINVAL -> InvalidArgument("Can not lock `$fd - $lock`, invalid argument")
        ENOLCK -> NoLock("Can not lock `$fd - $lock`, too many locks open")
        else -> InvalidArgument("Can not lock `$fd - $lock`: unknown error `$this`")
    }
}
