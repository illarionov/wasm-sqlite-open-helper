/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function

import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.SeekError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.SeekFd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Whence

public class FdSeekFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(WasiHostFunction.FD_SEEK, host) {
    public fun execute(
        memory: Memory,
        fd: Fd,
        offset: Long,
        whenceInt: Int,
        pNewOffset: WasmPtr<Long>,
    ): Errno {
        val whence = Whence.fromIdOrNull(whenceInt) ?: return Errno.INVAL
        return host.fileSystem.execute(
            SeekFd,
            SeekFd(fd = fd, fileDelta = offset, whence = whence),
        ).onLeft { error ->
            logger.i { "fdSeek() error: $error" }
        }.onRight { newPosition ->
            memory.writeI64(pNewOffset, newPosition)
        }.fold(
            ifLeft = SeekError::errno,
            ifRight = { _ -> Errno.SUCCESS },
        )
    }
}
