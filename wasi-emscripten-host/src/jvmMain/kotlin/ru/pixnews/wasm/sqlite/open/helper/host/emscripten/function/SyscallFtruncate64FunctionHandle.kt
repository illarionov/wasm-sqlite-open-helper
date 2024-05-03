/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

public class SyscallFtruncate64FunctionHandle(
    host: SqliteEmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.SYSCALL_FTRUNCATE64, host) {
    public fun execute(
        fd: Fd,
        length: ULong,
    ): Int = try {
        host.fileSystem.ftruncate(fd, length)
        Errno.SUCCESS.code
    } catch (e: SysException) {
        logger.v { "ftruncate64($fd, $length): Error ${e.errNo}" }
        -e.errNo.code
    }
}
