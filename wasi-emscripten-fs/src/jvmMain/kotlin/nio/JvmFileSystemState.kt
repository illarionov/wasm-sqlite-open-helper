/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.NioFileDescriptorTable
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd.JvmPathResolver
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd.PathResolver
import java.nio.file.FileSystems
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.nio.file.FileSystem as NioFileSystem

internal class JvmFileSystemState(
    rootLogger: Logger,
    val javaFs: NioFileSystem = FileSystems.getDefault(),
) : AutoCloseable {
    val fsLock: Lock = ReentrantLock()
    val fileDescriptors: NioFileDescriptorTable = NioFileDescriptorTable(this, rootLogger)
    val pathResolver: PathResolver = JvmPathResolver(javaFs, fileDescriptors)

    override fun close() {
        fileDescriptors.close()
    }
}
