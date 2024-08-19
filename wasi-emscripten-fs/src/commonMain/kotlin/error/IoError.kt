/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno

public data class IoError(
    override val message: String,
) : FileSystemOperationError,
    AdvisoryLockError,
    CheckAccessError,
    ChmodError,
    ChownError,
    CloseError,
    MkdirError,
    OpenError,
    ReadError,
    ReadLinkError,
    SetTimestampError,
    StatError,
    SyncError,
    TruncateError,
    UnlinkError,
    WriteError {
    override val errno: Errno = Errno.IO
}