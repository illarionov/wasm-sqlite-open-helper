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
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.encodeToNullTerminatedByteArray
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.pointer
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.WasiValueTypes.U8
import java.util.logging.Logger

internal fun syscallGetcwd(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "__syscall_getcwd",
    paramTypes = listOf(
        U8.pointer, // buf
        I32, // size
    ),
    returnType = Errno.wasmValueType,
    moduleName = moduleName,
    handle = Getcwd(filesystem),
)

private class Getcwd(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(Getcwd::class.qualifiedName),
) : EmscriptenHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Value {
        val result = getCwd(
            instance,
            args[0].asWasmAddr(),
            args[1].asInt(),
        )
        return Value.i32(result.toLong())
    }

    private fun getCwd(
        instance: Instance,
        dst: WasmPtr<WasmPtr<Byte>>,
        size: Int,
    ): Int {
        logger.finest { "getCwd(dst: 0x${dst.addr.toString(16)} size: $size)" }
        if (size == 0) {
            return -Errno.INVAL.code
        }

        val path = filesystem.getCwd()
        val pathBytes = path.encodeToNullTerminatedByteArray()

        if (size < pathBytes.size) {
            return -Errno.RANGE.code
        }
        instance.memory().write(dst.addr, pathBytes)

        return pathBytes.size
    }
}
