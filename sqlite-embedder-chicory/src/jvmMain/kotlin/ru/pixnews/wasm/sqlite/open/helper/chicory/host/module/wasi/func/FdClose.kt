/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.WasiHostFunction
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.wasiHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import java.util.logging.Level
import java.util.logging.Logger

internal fun fdClose(
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = wasiHostFunction(
    funcName = "fd_close",
    paramTypes = listOf(
        Fd.wasmValueType, // Fd
    ),
    moduleName = moduleName,
    handle = FdClose(filesystem),
)

private class FdClose(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(FdClose::class.qualifiedName),
) : WasiHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Errno {
        val fd = Fd(args[0].asInt())
        return try {
            filesystem.close(fd)
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.log(Level.INFO, e) { "fd_close() error: $e" }
            e.errNo
        }
    }
}
