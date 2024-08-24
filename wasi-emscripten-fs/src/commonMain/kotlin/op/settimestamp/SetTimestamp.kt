/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.settimestamp

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.SetTimestampError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory.CurrentWorkingDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation

public data class SetTimestamp(
    public val path: String,
    public val baseDirectory: BaseDirectory = CurrentWorkingDirectory,
    public val atimeNanoseconds: Long?,
    public val mtimeNanoseconds: Long?,
    public val followSymlinks: Boolean,
) {
   public companion object : FileSystemOperation<SetTimestamp, SetTimestampError, Unit> {
       override val tag: String = "setts"
   }
}
