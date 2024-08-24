/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.unlink

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.UnlinkError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation

public data class UnlinkFile(
    public val path: String,
    public val baseDirectory: BaseDirectory = BaseDirectory.CurrentWorkingDirectory,
) {
    public companion object : FileSystemOperation<UnlinkFile, UnlinkError, Unit> {
        override val tag: String = "unlinkfile"
    }
}
