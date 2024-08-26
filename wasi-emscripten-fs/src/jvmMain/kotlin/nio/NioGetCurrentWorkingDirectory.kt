/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import arrow.core.Either
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.GetCurrentWorkingDirectoryError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.delegatefs.FileSystemOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd.CurrentDirectoryProvider
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.cwd.GetCurrentWorkingDirectory
import java.nio.file.Path

internal class NioGetCurrentWorkingDirectory(
    private val currentDirectoryProvider: CurrentDirectoryProvider,
) : FileSystemOperationHandler<GetCurrentWorkingDirectory, GetCurrentWorkingDirectoryError, String> {
    override fun invoke(input: GetCurrentWorkingDirectory): Either<GetCurrentWorkingDirectoryError, String> {
        return currentDirectoryProvider.getCurrentWorkingDirectory().map(Path::toString)
    }
}
