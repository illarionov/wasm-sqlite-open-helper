/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.nio

import arrow.core.Either
import arrow.core.right
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.GetCurrentWorkingDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.GetCurrentWorkingDirectoryError

internal class NioGetCurrentWorkingDirectory(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<GetCurrentWorkingDirectory, GetCurrentWorkingDirectoryError, String> {
    override fun invoke(input: GetCurrentWorkingDirectory): Either<GetCurrentWorkingDirectoryError, String> {
        return fsState.currentWorkingDirectory.toString().right()
    }
}
