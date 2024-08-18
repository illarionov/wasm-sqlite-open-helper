/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.ResolvePathError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.ResolvePathError.EmptyPath
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.ResolvePathError.FileDescriptorNotOpen
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.ResolvePathError.InvalidPath
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.ResolvePathError.NotDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.ResolvePathError.RelativePath
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.resolvePath
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.toPosixFilePermissions
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.chmod.Chmod
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.chmod.ChmodError
import java.io.IOException
import java.nio.file.Path
import kotlin.io.path.setPosixFilePermissions

internal class NioChmod(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<Chmod, ChmodError, Unit> {
    override fun invoke(input: Chmod): Either<ChmodError, Unit> {
        val path: Path = fsState.resolvePath(input.path, input.baseDirectory, false)
            .mapLeft { it.toChmodError() }
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
                is UnsupportedOperationException -> ChmodError.PermissionDenied("Read-only channel")
                is ClassCastException -> ChmodError.InvalidArgument("Invalid flags")
                is IOException -> ChmodError.IoError("I/O exception: ${it.message}")
                is SecurityException -> ChmodError.AccessDenied("Security Exception")
                else -> throw IllegalStateException("Unexpected error", it)
            }
        }

        private fun ResolvePathError.toChmodError(): ChmodError = when (this) {
            is EmptyPath -> ChmodError.InvalidArgument(message)
            is FileDescriptorNotOpen -> ChmodError.BadFileDescriptor(message)
            is InvalidPath -> ChmodError.BadFileDescriptor(message)
            is NotDirectory -> ChmodError.NotDirectory(message)
            is RelativePath -> ChmodError.InvalidArgument(message)
        }
    }
}
