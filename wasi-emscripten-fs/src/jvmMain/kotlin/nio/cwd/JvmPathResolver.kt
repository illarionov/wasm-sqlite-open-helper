/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.asLinkOptions
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.NioFileDescriptorTable
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory.CurrentWorkingDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory.DirectoryFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory.None
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd.PathResolver.ResolvePathError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd.PathResolver.ResolvePathError.EmptyPath
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd.PathResolver.ResolvePathError.FileDescriptorNotOpen
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd.PathResolver.ResolvePathError.InvalidPath
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd.PathResolver.ResolvePathError.NotDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd.PathResolver.ResolvePathError.RelativePath
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString

@Suppress("ReturnCount")
internal class JvmPathResolver(
    private val javaFs: java.nio.file.FileSystem,
    private val fileDescriptors: NioFileDescriptorTable,
) : PathResolver {
    override fun resolve(
        path: String?,
        baseDirectory: BaseDirectory,
        allowEmptyPath: Boolean,
        followSymlinks: Boolean,
    ): Either<ResolvePathError, Path> {
        val nioPath = try {
            val pathString = path ?: ""
            javaFs.getPath(pathString)
        } catch (@Suppress("SwallowedException") ipe: InvalidPathException) {
            return InvalidPath("Path `$path` cannot be converted").left()
        }

        if (nioPath.pathString.isEmpty() && !allowEmptyPath) {
            return EmptyPath("Empty path is not allowed").left()
        }

        if (nioPath.isAbsolute) {
            return nioPath.right()
        }

        val baseDirectoryPath: Either<ResolvePathError, Path> = when (baseDirectory) {
            None -> RelativePath("Can not resolve `$path`: path should be absolute").left()
            CurrentWorkingDirectory -> javaFs.getPath("").right()
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
}
