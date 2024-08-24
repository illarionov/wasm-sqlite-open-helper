/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.stat

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.StatError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory.CurrentWorkingDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation

/**
 * Retrieves information about the file at the given [path].
 * If [path] is relative, it will be resolved using the [baseDirectory] provided.
 *
 * By default, all symbolic links are followed. If [followSymlinks] is set to false and [path] is a symbolic link,
 * the status of the symbolic link itself is returned instead of the target file.
 */
public data class Stat(
    val path: String,
    val baseDirectory: BaseDirectory = CurrentWorkingDirectory,
    val followSymlinks: Boolean = true,
) {
    public companion object : FileSystemOperation<Stat, StatError, StructStat> {
        override val tag: String = "stat"
    }
}
