/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.BaseDirectory.CurrentWorkingDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.BaseDirectory.DirectoryFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.BaseDirectory.None
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperationError
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.EmptyPath
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.FileDescriptorNotOpen
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.InvalidPath
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.NotDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.RelativePath
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.nio.JvmFileSystemState
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.BADF
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.INVAL
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.NOENT
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.NOTDIR
import java.nio.file.InvalidPathException
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString
import java.nio.file.Path as NioPath

@Suppress("SwallowedException", "ReturnCount")
internal fun JvmFileSystemState.resolvePath(
    path: String?,
    baseDirectory: BaseDirectory,
    allowEmptyPath: Boolean = false,
    followSymlinks: Boolean = true,
): Either<ResolvePathError, NioPath> {
    val nioPath = try {
        val pathString = path ?: ""
        javaFs.getPath(pathString)
    } catch (ipe: InvalidPathException) {
        return InvalidPath("Path `$path` cannot be converted").left()
    }

    if (nioPath.pathString.isEmpty() && !allowEmptyPath) {
        return EmptyPath("Empty path is not allowed").left()
    }

    if (nioPath.isAbsolute) {
        return nioPath.right()
    }

    val baseDirectoryPath: Either<ResolvePathError, NioPath> = when (baseDirectory) {
        None -> RelativePath("Can not resolve `$path`: path should be absolute").left()
        CurrentWorkingDirectory -> currentWorkingDirectory.right()
        is DirectoryFd -> {
            val fdPath = fileDescriptors.get(baseDirectory.fd)?.path
            fdPath?.right() ?: FileDescriptorNotOpen("File descriptor ${baseDirectory.fd} is not opened").left()
        }
    }.flatMap { basePath ->
        if (basePath.isDirectory(options = asLinkOptions(followSymlinks))) {
            basePath.right()
        } else {
            NotDirectory("Base path `$path` is not a directory").left()
        }
    }

    return baseDirectoryPath.map { it.resolve(nioPath) }
}

internal sealed class ResolvePathError : FileSystemOperationError {
    class RelativePath(
        override val message: String,
        override val errno: Errno = INVAL,
    ) : ResolvePathError()

    class InvalidPath(
        override val message: String,
        override val errno: Errno = INVAL,
    ) : ResolvePathError()

    class NotDirectory(
        override val message: String,
        override val errno: Errno = NOTDIR,
    ) : ResolvePathError()

    class FileDescriptorNotOpen(
        override val message: String,
        override val errno: Errno = BADF,
    ) : ResolvePathError()

    class EmptyPath(
        override val message: String,
        override val errno: Errno = NOENT,
    ) : ResolvePathError()
}
