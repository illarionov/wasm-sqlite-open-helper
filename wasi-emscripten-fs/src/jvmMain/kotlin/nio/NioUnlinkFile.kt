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
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.unlink.UnlinkError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.unlink.UnlinkFile
import java.io.IOException
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.isDirectory

internal class NioUnlinkFile(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<UnlinkFile, UnlinkError, Unit> {
    @Suppress("ReturnCount")
    override fun invoke(input: UnlinkFile): Either<UnlinkError, Unit> {
        val path: Path = fsState.resolvePath(input.path, input.baseDirectory, false)
            .mapLeft { it.toUnlinkError() }
            .getOrElse { return it.left() }

        if (path.isDirectory()) {
            return UnlinkError.PathIsDirectory("`$path` is a directory").left()
        }

        return Either.catch {
            Files.delete(path)
        }.mapLeft {
            it.toUnlinkError(path)
        }
    }

    companion object {
        internal fun ResolvePathError.toUnlinkError(): UnlinkError = when (this) {
            is EmptyPath -> UnlinkError.NoEntry(message)
            is FileDescriptorNotOpen -> UnlinkError.BadFileDescriptor(message)
            is InvalidPath -> UnlinkError.BadFileDescriptor(message)
            is NotDirectory -> UnlinkError.NotDirectory(message)
            is RelativePath -> UnlinkError.BadFileDescriptor(message)
        }

        internal fun Throwable.toUnlinkError(path: Path): UnlinkError = when (this) {
            is NoSuchFileException -> UnlinkError.NoEntry("No file `$path`")
            is DirectoryNotEmptyException -> UnlinkError.DirectoryNotEmpty("Directory not empty")
            is IOException -> UnlinkError.IoError("I/O Error: $message")
            else -> throw IllegalStateException("Unexpected error", this)
        }
    }
}
