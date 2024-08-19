/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.ext

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory.CurrentWorkingDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl

@InternalWasmSqliteHelperApi
public fun BaseDirectory.Companion.fromRawDirFd(rawDirFd: Int): BaseDirectory = when (rawDirFd) {
    Fcntl.AT_FDCWD -> CurrentWorkingDirectory
    else -> BaseDirectory.DirectoryFd(Fd(rawDirFd))
}
