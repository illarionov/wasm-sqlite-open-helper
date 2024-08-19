/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Exists
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.MkdirError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.PermissionDenied
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.ResolvePathError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.asFileAttribute
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.resolvePath
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.toCommonError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.toPosixFilePermissions
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.mkdir.Mkdir
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path

internal class NioMkdir(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<Mkdir, MkdirError, Unit> {
    override fun invoke(input: Mkdir): Either<MkdirError, Unit> {
        val path: Path = fsState.resolvePath(input.path, input.baseDirectory, false)
            .mapLeft(ResolvePathError::toCommonError)
            .getOrElse { return it.left() }
        return Either.catch {
            Files.createDirectory(path, input.mode.toPosixFilePermissions().asFileAttribute())
            Unit
        }.mapLeft {
            when (it) {
                is UnsupportedOperationException -> PermissionDenied("Unsupported file mode")
                is FileAlreadyExistsException -> Exists("`$path` exists")
                is IOException -> IoError("I/O error: ${it.message}")
                else -> throw IllegalStateException("Unexpected error", it)
            }
        }
    }
}
