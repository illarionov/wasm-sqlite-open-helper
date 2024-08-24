/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.lock

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.AdvisoryLockError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation

public data class AddAdvisoryLockFd(
    public val fd: Fd,

    public val flock: Advisorylock,
) {
    public companion object : FileSystemOperation<AddAdvisoryLockFd, AdvisoryLockError, Unit> {
        override val tag: String = "addlockfd"
    }
}
