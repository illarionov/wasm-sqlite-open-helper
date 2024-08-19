/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.checkaccess

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation

public data class CheckAccess(
    public val path: String,
    public val baseDirectory: BaseDirectory = BaseDirectory.CurrentWorkingDirectory,
    public val mode: Set<FileAccessibilityCheck>,
    public val useEffectiveUserId: Boolean = false,
    public val allowEmptyPath: Boolean = false,
    public val followSymlinks: Boolean = true,
) {
    public companion object : FileSystemOperation<CheckAccess, CheckAccessError, Unit>
}
