/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.resolvePath
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.NioUnlinkFile.Companion.toUnlinkError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.unlink.UnlinkError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.unlink.UnlinkFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory

internal class NioUnlinkDirectory(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<UnlinkFile, UnlinkError, Unit> {
    @Suppress("ReturnCount")
    override fun invoke(input: UnlinkFile): Either<UnlinkError, Unit> {
        val path: Path = fsState.resolvePath(input.path, input.baseDirectory, false)
            .mapLeft { it.toUnlinkError() }
            .getOrElse { return it.left() }

        if (!path.isDirectory()) {
            return UnlinkError.NotDirectory("`$path` is not a directory").left()
        }

        return Either.catch {
            Files.delete(path)
        }.mapLeft {
            it.toUnlinkError(path)
        }
    }
}
