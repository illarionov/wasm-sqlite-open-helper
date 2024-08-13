/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.nio

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.Mkdir
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.MkdirError
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.EmptyPath
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.FileDescriptorNotOpen
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.InvalidPath
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.NotDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.RelativePath
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.asFileAttribute
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.resolvePath
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.toPosixFilePermissions
import java.io.IOException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path

internal class NioMkdir(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<Mkdir, MkdirError, Unit> {
    override fun invoke(input: Mkdir): Either<MkdirError, Unit> {
        val path: Path = fsState.resolvePath(input.path, input.baseDirectory, false)
            .mapLeft { it.toMkdirError() }
            .getOrElse { return it.left() }
        return Either.catch {
            Files.createDirectory(path, input.mode.toPosixFilePermissions().asFileAttribute())
            Unit
        }.mapLeft {
            when (it) {
                is UnsupportedOperationException -> MkdirError.PermissionDenied("Unsupported file mode")
                is FileAlreadyExistsException -> MkdirError.Exist("`$path` exists")
                is IOException -> MkdirError.IoError("I/O error: ${it.message}")
                else -> throw IllegalStateException("Unexpected error", it)
            }
        }
    }

    internal companion object {
        fun ResolvePathError.toMkdirError(): MkdirError = when (this) {
            is EmptyPath -> MkdirError.InvalidArgument(message)
            is FileDescriptorNotOpen -> MkdirError.BadFileDescriptor(message)
            is InvalidPath -> MkdirError.InvalidArgument(message)
            is NotDirectory -> MkdirError.NotDirectory(message)
            is RelativePath -> MkdirError.InvalidArgument(message)
        }
    }
}
