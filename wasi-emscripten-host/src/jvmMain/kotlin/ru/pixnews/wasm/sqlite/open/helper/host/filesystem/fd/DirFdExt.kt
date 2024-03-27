/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd
import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd.Cwd
import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd.FileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.BADF
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.NOENT
import java.nio.file.Path
import kotlin.io.path.pathString

public fun FileSystem.resolveAbsolutePath(
    dirFd: DirFd,
    path: String,
    allowEmpty: Boolean = false,
): Path {
    return resolveAbsolutePath(dirFd, javaFs.getPath(path), allowEmpty)
}

public fun FileSystem.resolveAbsolutePath(
    dirFd: DirFd,
    path: Path,
    allowEmpty: Boolean = false,
): Path {
    if (path.isAbsolute) {
        return path
    }

    val root: Path = when (dirFd) {
        is Cwd -> getCwdPath()
        is FileDescriptor -> try {
            getStreamByFd(dirFd.fd).path
        } catch (e: SysException) {
            throw SysException(BADF, "File descriptor $dirFd is not open", cause = e)
        }
    }

    if (path.pathString.isEmpty() && !allowEmpty) {
        throw SysException(NOENT)
    }
    return root.resolve(path)
}
