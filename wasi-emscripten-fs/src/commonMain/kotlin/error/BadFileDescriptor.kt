/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno.BADF

public data class BadFileDescriptor(
    override val message: String,
) : FileSystemOperationError,
    AdvisoryLockError,
    ReadLinkError,
    CheckAccessError,
    ChmodError,
    ChownError,
    CloseError,
    MkdirError,
    OpenError,
    ReadError,
    ResolveRelativePathErrors,
    WriteError,
    SeekError,
    SetTimestampError,
    StatError,
    SyncError,
    TruncateError,
    UnlinkError {
    override val errno: Errno = BADF
}