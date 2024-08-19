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
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readlink.ReadLink
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readlink.ReadLinkError
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NotLinkException

internal class NioReadLink(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<ReadLink, ReadLinkError, String> {
    override fun invoke(input: ReadLink): Either<ReadLinkError, String> {
        val path: java.nio.file.Path = fsState.resolvePath(input.path, input.baseDirectory, false)
            .mapLeft { it.toReadLinkError() }
            .getOrElse { return it.left() }

        return Either.catch {
            Files.readSymbolicLink(path).toString()
        }.mapLeft {
            when (it) {
                is UnsupportedOperationException -> ReadLinkError.InvalidArgument("Symbolic links are not supported")
                is NotLinkException -> ReadLinkError.InvalidArgument("File `$path` is not a symlink")
                is IOException -> ReadLinkError.IoError("I/o error while read symbolink link of `$path`")
                is SecurityException -> ReadLinkError.AccessDenied("Permission denied `$path`")
                else -> throw IllegalStateException("Unexpected error", it)
            }
        }
    }

    private fun ResolvePathError.toReadLinkError(): ReadLinkError = when (this) {
        is EmptyPath -> ReadLinkError.InvalidArgument(message)
        is FileDescriptorNotOpen -> ReadLinkError.BadFileDescriptor(message)
        is InvalidPath -> ReadLinkError.InvalidArgument(message)
        is NotDirectory -> ReadLinkError.NotDirectory(message)
        is RelativePath -> ReadLinkError.InvalidArgument(message)
    }
}
