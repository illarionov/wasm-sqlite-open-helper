/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.linux

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import platform.posix.F_SETLK
import platform.posix.F_UNLCK
import platform.posix.errno
import platform.posix.fcntl
import platform.posix.flock
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.AdvisoryLockError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.delegatefs.FileSystemOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.linux.LinuxAddAdvisoryLockFd.errnoToAdvisoryLockError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.linux.LinuxAddAdvisoryLockFd.setFromAdvisoryLock
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.lock.RemoveAdvisoryLockFd

internal object LinuxRemoveAdvisoryLockFd : FileSystemOperationHandler<RemoveAdvisoryLockFd, AdvisoryLockError, Unit> {
    override fun invoke(input: RemoveAdvisoryLockFd): Either<AdvisoryLockError, Unit> = memScoped {
        val structFlockInstance: flock = alloc<flock> {
            setFromAdvisoryLock(input.flock)
            l_type = F_UNLCK.toShort()
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
}
