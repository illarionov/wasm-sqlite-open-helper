/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.nio

import arrow.core.Either
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.StatError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.StatFd
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.nio.NioStat.Companion.statCatching
import java.nio.file.Path

internal class NioStatFd(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<StatFd, StatError, StructStat> {
    override fun invoke(input: StatFd): Either<StatError, StructStat> {
        val path: Path = fsState.fileDescriptors.get(input.fd)?.path
            ?: return StatError.BadFileDescriptor("File descriptor `${input.fd}` is not opened").left()
        return statCatching(path, true)
    }
}
