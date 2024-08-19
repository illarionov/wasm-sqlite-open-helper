/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.sync

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.SyncError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation

public data class SyncFd(
    public val fd: Fd,
    public val syncMetadata: Boolean = true,
) {
   public companion object : FileSystemOperation<SyncFd, SyncError, Unit>
}