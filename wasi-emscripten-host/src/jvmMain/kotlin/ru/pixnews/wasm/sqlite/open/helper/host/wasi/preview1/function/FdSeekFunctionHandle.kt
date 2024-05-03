/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.position
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Whence

public class FdSeekFunctionHandle(
    host: SqliteEmbedderHost,
) : HostFunctionHandle(WasiHostFunction.FD_SEEK, host) {
    public fun execute(
        memory: Memory,
        fd: Fd,
        offset: Long,
        whenceInt: Int,
        pNewOffset: WasmPtr<Long>,
    ): Errno {
        val whence = Whence.fromIdOrNull(whenceInt) ?: return Errno.INVAL
        return try {
            val channel: FdChannel = host.fileSystem.getStreamByFd(fd)
            host.fileSystem.seek(channel, offset, whence)

            val newPosition = channel.position

            memory.writeI64(pNewOffset, newPosition)

            Errno.SUCCESS
        } catch (sysException: SysException) {
            logger.i(sysException) { "fdSeek() error" }
            sysException.errNo
        }
    }
}
