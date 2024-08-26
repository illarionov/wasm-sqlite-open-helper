/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.linux

import arrow.core.Either
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystemInterceptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.FileSystemOperationError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.delegatefs.DelegateOperationsFileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.delegatefs.FileSystemOperationHandler
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation
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
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.opencreate.Open
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
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.PosixCloseFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base.PosixFileSystemState

public class LinuxFileSystemImpl(
    interceptors: List<FileSystemInterceptor>,
) : FileSystem {
    private val fsState = PosixFileSystemState()
    private val operations: Map<FileSystemOperation<*, *, *>, FileSystemOperationHandler<*, *, *>> = mapOf(
        Open to LinuxOpen(fsState),
        CloseFd to PosixCloseFd(fsState),
        AddAdvisoryLockFd to LinuxAddAdvisoryLockFd,
        RemoveAdvisoryLockFd to LinuxRemoveAdvisoryLockFd,
        CheckAccess to LinuxCheckAccess,
        Chmod to LinuxChmod,
        ChmodFd to LinuxChmodFd,
        Chown to LinuxChown,
        ChownFd to LinuxChownFd,
        GetCurrentWorkingDirectory to LinuxGetCurrentWorkingDirectory,
        Mkdir to LinuxMkdir,
        ReadFd to LinuxReadFd,
        ReadLink to LinuxReadLink,
        SeekFd to LinuxSeekFd,
        SetTimestamp to LinuxSetTimestamp,
        SetTimestampFd to LinuxSetTimestampFd,
        Stat to LinuxStat,
        StatFd to LinuxStatFd,
        SyncFd to LinuxSync,
        TruncateFd to LinuxTruncateFd,
        UnlinkFile to LinuxUnlinkFile,
        UnlinkDirectory to LinuxUnlinkDirectory,
        WriteFd to LinuxWriteFd,
    )
    private val fsAdapter = DelegateOperationsFileSystem(operations, interceptors)

    override fun close() {
        fsState.close()
    }

    override fun <I : Any, E : FileSystemOperationError, R : Any> execute(
        operation: FileSystemOperation<I, E, R>,
        input: I,
    ): Either<E, R> = fsAdapter.execute(operation, input)

    override fun isOperationSupported(operation: FileSystemOperation<*, *, *>): Boolean {
        return fsAdapter.isOperationSupported(operation)
    }
}
