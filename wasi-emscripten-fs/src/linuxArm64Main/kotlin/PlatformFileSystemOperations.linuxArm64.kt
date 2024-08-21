/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.chmod.ChmodFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.chown.ChownFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.close.CloseFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.cwd.GetCurrentWorkingDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.lock.AddAdvisoryLockFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.lock.RemoveAdvisoryLockFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.opencreate.Open
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.WriteFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.seek.SeekFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.settimestamp.SetTimestampFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat.StatFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.sync.SyncFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.truncate.TruncateFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.LinuxAddAdvisoryLockFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.LinuxArm64StatFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.LinuxChmodFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.LinuxChownFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.LinuxGetCurrentWorkingDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.LinuxOpen
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.LinuxReadFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.LinuxRemoveAdvisoryLockFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.LinuxSeekFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.LinuxSetTimestampFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.LinuxSync
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.LinuxTruncateFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.LinuxWriteFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.PosixCloseFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base.PosixFileSystemState
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base.PosixOperationHandler

internal actual fun createPlatformFileSystemOperations(
    fsState: PosixFileSystemState,
): Map<FileSystemOperation<*, *, *>, PosixOperationHandler<*, *, *>> = mapOf(
    Open to LinuxOpen(fsState),
    CloseFd to PosixCloseFd(fsState),
    AddAdvisoryLockFd to LinuxAddAdvisoryLockFd,
    RemoveAdvisoryLockFd to LinuxRemoveAdvisoryLockFd,
    ChmodFd to LinuxChmodFd,
    ChownFd to LinuxChownFd,
    GetCurrentWorkingDirectory to LinuxGetCurrentWorkingDirectory,
    ReadFd to LinuxReadFd,
    SeekFd to LinuxSeekFd,
    SetTimestampFd to LinuxSetTimestampFd,
    StatFd to LinuxArm64StatFd,
    SyncFd to LinuxSync,
    TruncateFd to LinuxTruncateFd,
    WriteFd to LinuxWriteFd,
)
