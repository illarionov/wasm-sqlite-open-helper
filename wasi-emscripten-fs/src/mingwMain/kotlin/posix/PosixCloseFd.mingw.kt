/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.CloseError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd

internal actual fun Int.platformSpecificErrnoToCloseError(fd: Fd): CloseError {
    return IoError("Unknown error $this while closing $fd")
}
