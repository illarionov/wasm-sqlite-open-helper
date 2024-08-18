/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.common

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperationError

internal sealed class ChannelPositionError(
    override val errno: Errno,
    override val message: String,
) : FileSystemOperationError {
    internal data class ClosedChannel(override val message: String) : ChannelPositionError(Errno.BADF, message)
    internal data class IoError(override val message: String) : ChannelPositionError(Errno.IO, message)
    internal data class InvalidArgument(override val message: String) : ChannelPositionError(Errno.INVAL, message)
}
