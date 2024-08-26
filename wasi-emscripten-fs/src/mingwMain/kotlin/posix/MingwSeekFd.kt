/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import platform.posix.errno
import platform.posix.lseek
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Overflow
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.SeekError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.delegatefs.FileSystemOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.seek.SeekFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.ext.errnoToSeekError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.ext.toPosixWhence

internal object MingwSeekFd : FileSystemOperationHandler<SeekFd, SeekError, Long> {
    override fun invoke(input: SeekFd): Either<SeekError, Long> {
        if (input.fileDelta > Int.MAX_VALUE) {
            return Overflow("input.fileDelta too big. Request: $input").left()
        }

        val offset = lseek(input.fd.fd, input.fileDelta.toInt(), input.whence.toPosixWhence())

        return if (offset >= 0) {
            offset.toLong().right()
        } else {
            errno.errnoToSeekError(input).left()
        }
    }
}
