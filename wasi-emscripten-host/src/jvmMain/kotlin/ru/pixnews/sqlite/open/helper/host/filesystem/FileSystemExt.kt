/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.host.filesystem

import ru.pixnews.sqlite.open.helper.host.include.Fcntl.AT_FDCWD
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.Errno.BADF
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.Errno.NOENT
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.Fd
import java.nio.file.Path
import kotlin.io.path.pathString

public fun FileSystem.resolveAbsolutePath(
    dirFd: Int,
    path: String,
    allowEmpty: Boolean = false,
): Path {
    return resolveAbsolutePath(dirFd, javaFs.getPath(path), allowEmpty)
}

public fun FileSystem.resolveAbsolutePath(
    dirFd: Int,
    path: Path,
    allowEmpty: Boolean = false,
): Path {
    if (path.isAbsolute) {
        return path
    }

    val root: Path = if (dirFd == AT_FDCWD) {
        getCwdPath()
    } else {
        try {
            getStreamByFd(Fd(dirFd)).path
        } catch (e: SysException) {
            throw SysException(BADF, "File descriptor $dirFd is not open", cause = e)
        }
    }

    if (path.pathString.isEmpty() && !allowEmpty) {
        throw SysException(NOENT)
    }
    return root.resolve(path)
}
