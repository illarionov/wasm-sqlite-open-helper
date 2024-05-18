/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.JvmPath
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.BADF
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.INVAL
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.IO
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import java.io.IOException
import java.nio.channels.ClosedChannelException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

internal class FdFileChannel(
    val fileSystem: FileSystem<JvmPath>,
    val fd: Fd,
    val path: JvmPath,

    val channel: FileChannel,
) {
    val lock: Lock = ReentrantLock()
    val fileLocks: MutableMap<FileLockKey, FileLock> = mutableMapOf()
}

internal var FdFileChannel.position: Long
    get() = try {
        channel.position()
    } catch (ce: ClosedChannelException) {
        throw SysException(BADF, cause = ce)
    } catch (ioe: IOException) {
        throw SysException(IO, cause = ioe)
    }
    set(newPosition) = try {
        channel.position(newPosition)
        Unit
    } catch (ce: ClosedChannelException) {
        throw SysException(BADF, cause = ce)
    } catch (ioe: IOException) {
        throw SysException(IO, cause = ioe)
    } catch (iae: IllegalArgumentException) {
        throw SysException(INVAL, "Negative new position", cause = iae)
    }
