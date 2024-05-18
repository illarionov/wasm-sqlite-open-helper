/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysMmanMapFlags
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysMmanProt
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

public class MmapJsFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.MMAP_JS, host) {
    public fun execute(
        len: Int,
        prot: SysMmanProt,
        flags: SysMmanMapFlags,
        fd: Fd,
        offset: ULong,
        pAllocated: WasmPtr<Int>,
        pAddr: WasmPtr<WasmPtr<Byte>>,
    ): Int {
        logger.v { "mmapJs($fd, $len, $prot, $flags, $fd, $offset, $pAllocated, $pAddr): Not implemented" }

        return try {
            if (!host.fileSystem.isRegularFile(fd)) {
                throw SysException(Errno.NODEV, "${host.fileSystem.getPath(fd)} is not a file")
            }
            return -Errno.INVAL.code // Not Supported
        } catch (e: SysException) {
            logger.v { "mmapJs($fd, $len, $prot, $flags, $fd, $offset, $pAllocated, $pAddr): Error ${e.errNo}" }
            -e.errNo.code
        }
    }
}
