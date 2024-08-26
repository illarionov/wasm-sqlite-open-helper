/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.DirectoryNotEmpty
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NoEntry
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.PathIsDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.UnlinkError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.delegatefs.FileSystemOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd.PathResolver
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd.PathResolver.ResolvePathError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.unlink.UnlinkFile
import java.io.IOException
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.isDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NotDirectory as BaseNotDirectory

internal class NioUnlinkFile(
    private val pathResolver: PathResolver,
) : FileSystemOperationHandler<UnlinkFile, UnlinkError, Unit> {
    @Suppress("ReturnCount")
    override fun invoke(input: UnlinkFile): Either<UnlinkError, Unit> {
        val path: Path = pathResolver.resolve(input.path, input.baseDirectory, false)
            .mapLeft { it.toUnlinkError() }
            .getOrElse { return it.left() }

        if (path.isDirectory()) {
            return PathIsDirectory("`$path` is a directory").left()
        }

        return Either.catch {
            Files.delete(path)
        }.mapLeft {
            it.toUnlinkError(path)
        }
    }

    companion object {
        internal fun ResolvePathError.toUnlinkError(): UnlinkError = when (this) {
            is ResolvePathError.EmptyPath -> NoEntry(message)
            is ResolvePathError.FileDescriptorNotOpen -> BadFileDescriptor(message)
            is ResolvePathError.InvalidPath -> BadFileDescriptor(message)
            is ResolvePathError.NotDirectory -> BaseNotDirectory(message)
            is ResolvePathError.RelativePath -> BadFileDescriptor(message)
        }

        internal fun Throwable.toUnlinkError(path: Path): UnlinkError = when (this) {
            is NoSuchFileException -> NoEntry("No file `$path`")
            is DirectoryNotEmptyException -> DirectoryNotEmpty("Directory not empty")
            is IOException -> IoError("I/O Error: $message")
            else -> throw IllegalStateException("Unexpected error", this)
        }
    }
}
