/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import arrow.core.Either
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.ChmodError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.NioChmod.Companion.setPosixFilePermissions
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.chmod.ChmodFd
import java.nio.file.Path

internal class NioChmodFd(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<ChmodFd, ChmodError, Unit> {
    override fun invoke(input: ChmodFd): Either<ChmodError, Unit> {
        val path: Path = fsState.fileDescriptors.get(input.fd)?.path
            ?: return BadFileDescriptor("File descriptor `${input.fd}` is not opened").left()
        return setPosixFilePermissions(path, input.mode)
    }
}
