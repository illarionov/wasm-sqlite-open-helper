/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysMmanMapFlags
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysMmanProt
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.NODEV
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import kotlin.io.path.isRegularFile

public class MunapJsFunctionHandle(
    host: SqliteEmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.MUNMAP_JS, host) {
    public fun execute(
        addr: WasmPtr<Byte>,
        len: Int,
        prot: SysMmanProt,
        flags: SysMmanMapFlags,
        fd: Fd,
        offset: ULong,
    ): Int {
        logger.v { "munmapJs($addr, $len, $prot, $flags, $fd, $offset): Not implemented" }

        return try {
            val channel = host.fileSystem.getStreamByFd(fd)
            if (!channel.path.isRegularFile()) {
                throw SysException(NODEV, "${channel.path} is not a file")
            }
            return -Errno.INVAL.code // Not Supported
        } catch (e: SysException) {
            logger.v { "munmapJs($addr, $len, $prot, $flags, $fd, $offset): Error ${e.errNo}" }
            -e.errNo.code
        }
    }
}
