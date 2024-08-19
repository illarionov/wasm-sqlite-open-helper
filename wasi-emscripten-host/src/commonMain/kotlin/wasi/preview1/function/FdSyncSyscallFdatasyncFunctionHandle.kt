/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function

import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.sync.SyncFd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction

public class FdSyncSyscallFdatasyncFunctionHandle private constructor(
    host: EmbedderHost,
    function: HostFunction,
    private val syncMetadata: Boolean,
) : HostFunctionHandle(function, host) {
    public fun execute(
        fd: Fd,
    ): Errno = host.fileSystem.execute(SyncFd, SyncFd(fd, syncMetadata))
        .onLeft { error ->
            logger.i { "sync() error: $error" }
        }
        .fold(
            ifLeft = { it.errno },
            ifRight = { Errno.SUCCESS },
        )

    public companion object {
        public fun fdSync(
            host: EmbedderHost,
        ): FdSyncSyscallFdatasyncFunctionHandle = FdSyncSyscallFdatasyncFunctionHandle(
            host = host,
            function = WasiHostFunction.FD_SYNC,
            syncMetadata = true,
        )

        public fun syscallFdatasync(
            host: EmbedderHost,
        ): FdSyncSyscallFdatasyncFunctionHandle = FdSyncSyscallFdatasyncFunctionHandle(
            host = host,
            function = EmscriptenHostFunction.SYSCALL_FDATASYNC,
            syncMetadata = false,
        )
    }
}
