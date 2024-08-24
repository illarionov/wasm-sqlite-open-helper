/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.ext

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory.CurrentWorkingDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory.DirectoryFd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory.None
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.AT_FDCWD

internal fun BaseDirectory.toDirFd(): Int = when (this) {
    None -> AT_FDCWD
    CurrentWorkingDirectory -> AT_FDCWD
    is DirectoryFd -> this.fd.fd
}
