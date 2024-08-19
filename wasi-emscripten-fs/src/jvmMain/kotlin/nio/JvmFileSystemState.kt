/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.NioFileDescriptorTable
import java.nio.file.FileSystems
import java.nio.file.Path
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

internal class JvmFileSystemState(
    rootLogger: Logger,
    val javaFs: java.nio.file.FileSystem = FileSystems.getDefault(),
) : AutoCloseable {
    val fsLock: Lock = ReentrantLock()
    val fileDescriptors: NioFileDescriptorTable = NioFileDescriptorTable(this, rootLogger)

    val currentWorkingDirectory: Path
        get() = javaFs.getPath("").toAbsolutePath()

    override fun close() {
        fileDescriptors.close()
    }
}
