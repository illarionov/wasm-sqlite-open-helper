/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.ext

import platform.posix.EBADF
import platform.posix.EINVAL
import platform.posix.ENXIO
import platform.posix.EOVERFLOW
import platform.posix.ESPIPE
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Nxio
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Overflow
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Pipe
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.SeekError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.seek.SeekFd

internal fun Int.errnoToSeekError(request: SeekFd): SeekError = when (this) {
    EBADF -> BadFileDescriptor("Bad file descriptor ${request.fd}")
    EINVAL -> InvalidArgument("Whence is not valid. Request: $request")
    ENXIO -> Nxio("Invalid offset. Request: $request")
    EOVERFLOW -> Overflow("Resulting offset is out of range. Request: $request")
    ESPIPE -> Pipe("${request.fd} is not a file")
    else -> InvalidArgument("Other error. Errno: $this")
}
