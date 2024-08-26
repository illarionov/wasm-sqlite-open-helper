/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.AccessDenied
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.ChmodError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.PermissionDenied
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.toPosixFilePermissions
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.delegatefs.FileSystemOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd.PathResolver.ResolvePathError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd.toCommonError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.chmod.Chmod
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.setPosixFilePermissions

internal class NioChmod(
    private val fsState: NioFileSystemState,
) : FileSystemOperationHandler<Chmod, ChmodError, Unit> {
    override fun invoke(input: Chmod): Either<ChmodError, Unit> {
        val path: Path = fsState.pathResolver.resolve(input.path, input.baseDirectory, false)
            .mapLeft(ResolvePathError::toCommonError)
            .getOrElse { return it.left() }
        return setPosixFilePermissions(path, input.mode)
    }

    companion object {
        fun setPosixFilePermissions(
            path: Path,
            mode: FileMode,
        ): Either<ChmodError, Unit> = Either.catch {
            path.setPosixFilePermissions(mode.toPosixFilePermissions())
            Unit
        }.mapLeft {
            when (it) {
                is UnsupportedOperationException -> PermissionDenied("Read-only channel")
                is ClassCastException -> InvalidArgument("Invalid flags")
                is IOException -> IoError("I/O exception: ${it.message}")
                is SecurityException -> AccessDenied("Security Exception")
                else -> throw IllegalStateException("Unexpected error", it)
            }
        }
    }
}
