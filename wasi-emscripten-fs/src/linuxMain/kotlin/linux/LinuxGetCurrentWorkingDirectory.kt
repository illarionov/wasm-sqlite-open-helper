/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.linux

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.cinterop.ByteVarOf
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.EACCES
import platform.posix.EINVAL
import platform.posix.ENAMETOOLONG
import platform.posix.ENOENT
import platform.posix.PATH_MAX
import platform.posix.errno
import platform.posix.getcwd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.AccessDenied
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.GetCurrentWorkingDirectoryError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NameTooLong
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NoEntry
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.delegatefs.FileSystemOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.cwd.GetCurrentWorkingDirectory

internal object LinuxGetCurrentWorkingDirectory :
    FileSystemOperationHandler<GetCurrentWorkingDirectory, GetCurrentWorkingDirectoryError, String> {
    override fun invoke(input: GetCurrentWorkingDirectory): Either<GetCurrentWorkingDirectoryError, String> {
        val byteArray = ByteArray(PATH_MAX)
        val cwd: CPointer<ByteVarOf<Byte>>? = byteArray.usePinned { bytes ->
            getcwd(bytes.addressOf(0), PATH_MAX.toULong())
        }
        return if (cwd != null) {
            byteArray.decodeToString().right()
        } else {
            errno.errnoToGetCurrentWorkingDirectoryError().left()
        }
    }

    private fun Int.errnoToGetCurrentWorkingDirectoryError(): GetCurrentWorkingDirectoryError = when (this) {
        EACCES -> AccessDenied("Access denied")
        EINVAL -> InvalidArgument("Invalid argument")
        ENAMETOOLONG -> NameTooLong("Current working directory name too long")
        ENOENT -> NoEntry("Current working directory has been unlinked")
        else -> InvalidArgument("Unexpected error `$this`")
    }
}
