/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.emscriptenEnvHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import java.util.logging.Logger

internal fun syscallFchown32(
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "__syscall_fchown32",
    paramTypes = listOf(
        Fd.wasmValueType, // fd
        I32, // owner,
        I32, // group,
    ),
    returnType = I32,
    moduleName = moduleName,
    handle = Fchown32(filesystem),
)

private class Fchown32(
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(Fchown32::class.qualifiedName),
) : EmscriptenHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Value {
        val fd = Fd(args[0].asInt())
        val owner = args[1].asInt()
        val group = args[2].asInt()
        val code = try {
            filesystem.chown(fd, owner, group)
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.finest { "chown($fd, $owner, $group): Error ${e.errNo}" }
            e.errNo
        }
        return Value.i32(-code.code.toLong())
    }
}
