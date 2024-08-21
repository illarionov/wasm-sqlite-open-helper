/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.opencreate

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.OpenError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation

/**
 * Open or create a	file.
 *
 * The path to the file is specified by the [path]. If is relative, it will be resolved using the base directory
 * specified by the [baseDirectory] parameter.
 *
 * The [flags] argument may indicate the file is to be created if it does not exist (by specifying  the
 * [Fcntl.O_CREAT]	flag). In this case, file is created with mode [mode].
 */
public data class Open(
    public val path: String,
    public val baseDirectory: BaseDirectory = BaseDirectory.CurrentWorkingDirectory,
    public val flags: OpenFileFlags,
    public val mode: FileMode = FileMode(0U),
) {
    override fun toString(): String = "Open(" +
            "path=$path, " +
            "baseDirectory=$baseDirectory, " +
            "flags=$flags" +
            "mode=$mode" +
            ")"

    public companion object : FileSystemOperation<Open, OpenError, Fd>
}
