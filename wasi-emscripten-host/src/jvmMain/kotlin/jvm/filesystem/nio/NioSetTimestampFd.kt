/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.nio

import arrow.core.Either
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.SetTimestampError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.SetTimestampFd
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.nio.NioSetTimestamp.Companion.setTimestamp
import java.nio.file.Path

internal class NioSetTimestampFd(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<SetTimestampFd, SetTimestampError, Unit> {
    override fun invoke(input: SetTimestampFd): Either<SetTimestampError, Unit> {
        val path: Path = fsState.fileDescriptors.get(input.fd)?.path
            ?: return SetTimestampError.BadFileDescriptor("File descriptor `${input.fd}` is not opened").left()
        return setTimestamp(path, input.followSymlinks, input.atimeNanoseconds, input.mtimeNanoseconds)
    }
}
