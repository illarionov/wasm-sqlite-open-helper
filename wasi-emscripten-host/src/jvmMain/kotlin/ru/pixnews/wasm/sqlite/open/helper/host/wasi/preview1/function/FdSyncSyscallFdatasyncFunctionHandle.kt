/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function

import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

public class FdSyncSyscallFdatasyncFunctionHandle private constructor(
    host: SqliteEmbedderHost,
    function: HostFunction,
    private val syncMetadata: Boolean,
) : HostFunctionHandle(function, host) {
    public fun execute(
        fd: Fd,
    ): Errno {
        return try {
            host.fileSystem.sync(fd, metadata = syncMetadata)
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.i(e) { "sync() error" }
            e.errNo
        }
    }

    public companion object {
        public fun fdSync(
            host: SqliteEmbedderHost,
        ): FdSyncSyscallFdatasyncFunctionHandle = FdSyncSyscallFdatasyncFunctionHandle(
            host = host,
            function = WasiHostFunction.FD_SYNC,
            syncMetadata = true,
        )

        public fun syscallFdatasync(
            host: SqliteEmbedderHost,
        ): FdSyncSyscallFdatasyncFunctionHandle = FdSyncSyscallFdatasyncFunctionHandle(
            host = host,
            function = EmscriptenHostFunction.SYSCALL_FDATASYNC,
            syncMetadata = false,
        )
    }
}
