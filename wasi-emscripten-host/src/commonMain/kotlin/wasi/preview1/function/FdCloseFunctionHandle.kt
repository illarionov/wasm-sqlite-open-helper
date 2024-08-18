/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function

import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.close.CloseFd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction

public class FdCloseFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(WasiHostFunction.FD_CLOSE, host) {
    public fun execute(
        fd: Fd,
    ): Errno = host.fileSystem.execute(CloseFd, CloseFd(fd))
        .onLeft { error ->
            logger.i { "fd_close() error: $error" }
        }
        .fold(
            ifLeft = { it.errno },
            ifRight = { Errno.SUCCESS },
        )
}
