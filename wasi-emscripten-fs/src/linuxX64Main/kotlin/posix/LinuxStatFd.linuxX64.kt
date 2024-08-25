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
import kotlinx.cinterop.ptr
import platform.posix.errno
import platform.posix.fstat
import platform.posix.stat
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.StructStat

internal actual fun platformFstatFd(fd: Int): Either<Int, StructStat> = memScoped {
    val statBuf: stat = alloc()
    val exitCode = fstat(
        fd,
        statBuf.ptr,
    )
    return if (exitCode == 0) {
        statBuf.toStructStat().right()
    } else {
        errno.left()
    }
}
