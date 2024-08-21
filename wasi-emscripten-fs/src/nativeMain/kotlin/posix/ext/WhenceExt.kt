/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.ext

import platform.posix.SEEK_CUR
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Whence
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Whence.CUR
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Whence.END
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Whence.SET

internal fun Whence.toPosixWhence(): Int = when (this) {
    SET -> SEEK_SET
    CUR -> SEEK_CUR
    END -> SEEK_END
}
