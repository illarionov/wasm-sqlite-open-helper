/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MemberNameEqualsClassName")

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.emscriptenEnvHostFunction
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.pack
import ru.pixnews.wasm.sqlite.open.helper.host.pointer
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.WasiValueTypes.U8
import java.util.logging.Logger

internal fun syscallFstat64(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "__syscall_fstat64",
    paramTypes = listOf(
        Fd.wasmValueType,
        U8.pointer, // statbuf
    ),
    returnType = Errno.wasmValueType,
    moduleName = moduleName,
    handle = Fstat64(filesystem),
)

private class Fstat64(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(Fstat64::class.qualifiedName),
) : EmscriptenHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Value {
        val result = fstat64(
            instance,
            Fd(args[0].asInt()),
            args[1].asWasmAddr(),
        )
        return Value.i32(result.toLong())
    }

    private fun fstat64(
        instance: Instance,
        fd: Fd,
        dst: WasmPtr<StructStat>,
    ): Int {
        try {
            val stat = filesystem.stat(fd).also {
                logger.finest { "fStat64($fd): OK $it" }
            }.pack()
            instance.memory().write(dst.addr, stat)
        } catch (e: SysException) {
            logger.finest { "fStat64($fd): Error ${e.errNo}" }
            return -e.errNo.code
        }
        return 0
    }
}
