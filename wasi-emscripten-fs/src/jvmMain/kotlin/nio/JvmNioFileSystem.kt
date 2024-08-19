/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import arrow.core.Either
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.op.RunWithChannelFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.FileSystemOperationError
import error.NotImplemented
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.checkaccess.CheckAccess
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.chmod.Chmod
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.chmod.ChmodFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.chown.Chown
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.chown.ChownFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.close.CloseFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.cwd.GetCurrentWorkingDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.lock.AddAdvisoryLockFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.lock.RemoveAdvisoryLockFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.mkdir.Mkdir
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.open.Open
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readlink.ReadLink
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.WriteFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.seek.SeekFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.settimestamp.SetTimestamp
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.settimestamp.SetTimestampFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.Stat
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.StatFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.sync.SyncFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.truncate.TruncateFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.unlink.UnlinkDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.unlink.UnlinkFile
import java.nio.file.FileSystems

public class JvmNioFileSystem(
    javaFs: java.nio.file.FileSystem = FileSystems.getDefault(),
    rootLogger: Logger = Logger,
) : FileSystem {
    private val fsState = JvmFileSystemState(rootLogger, javaFs)
    private val operations: Map<FileSystemOperation<*, *, *>, NioOperationHandler<*, *, *>> = mapOf(
        Open to NioOpen(fsState),
        CloseFd to NioCloseFd(fsState),
        AddAdvisoryLockFd to NioAddAdvisoryLockFd(fsState),
        RemoveAdvisoryLockFd to NioRemoveAdvisoryLockFd(fsState),
        CheckAccess to NioCheckAccess(fsState),
        Chmod to NioChmod(fsState),
        ChmodFd to NioChmodFd(fsState),
        Chown to NioChown(fsState),
        ChownFd to NioChownFd(fsState),
        GetCurrentWorkingDirectory to NioGetCurrentWorkingDirectory(fsState),
        Mkdir to NioMkdir(fsState),
        ReadFd to NioReadFd(fsState),
        ReadLink to NioReadLink(fsState),
        SeekFd to NioSeekFd(fsState),
        SetTimestamp to NioSetTimestamp(fsState),
        SetTimestampFd to NioSetTimestampFd(fsState),
        Stat to NioStat(fsState),
        StatFd to NioStatFd(fsState),
        SyncFd to NioSync(fsState),
        TruncateFd to NioTruncateFd(fsState),
        UnlinkFile to NioUnlinkFile(fsState),
        UnlinkDirectory to NioUnlinkDirectory(fsState),
        WriteFd to NioWriteFd(fsState),
        RunWithChannelFd to NioRunWithRawChannelFd<Any>(fsState),
    )

    override fun close() {
        fsState.close()
    }

    override fun isOperationSupported(
        operation: FileSystemOperation<*, *, *>,
    ): Boolean = operations.containsKey(operation)

    @Suppress("UNCHECKED_CAST")
    override fun <I : Any, E : FileSystemOperationError, R : Any> execute(
        operation: FileSystemOperation<I, E, R>,
        input: I,
    ): Either<E, R> {
        val handler = operations[operation] as NioOperationHandler<I, E, R>?
        if (handler == null) {
            return NotImplemented.left() as Either<E, R>
        }

        return handler.invoke(input)
    }
}
