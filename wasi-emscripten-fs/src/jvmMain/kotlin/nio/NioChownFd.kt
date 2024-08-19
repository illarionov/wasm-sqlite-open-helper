/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import arrow.core.Either
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.NioChown.Companion.setPosixUserGroup
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.chown.ChownError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.chown.ChownFd
import java.nio.file.Path

internal class NioChownFd(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<ChownFd, ChownError, Unit> {
    override fun invoke(input: ChownFd): Either<ChownError, Unit> {
        val path: Path = fsState.fileDescriptors.get(input.fd)?.path
            ?: return ChownError.BadFileDescriptor("File descriptor `${input.fd}` is not opened").left()
        return setPosixUserGroup(fsState.javaFs, path, input.owner, input.group)
    }
}
