/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.emscriptenEnvHostFunction
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readZeroTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno

internal fun syscallUnlinkat(
    memory: Memory,
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "__syscall_unlinkat",
    paramTypes = listOf(
        I32, // dirfd
        I32, // pathname
        I32, // flags
    ),
    returnType = I32,
    moduleName = moduleName,
    handle = Unlinkat(memory, filesystem),
)

private class Unlinkat(
    private val memory: Memory,
    private val filesystem: FileSystem,
) : EmscriptenHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Value {
        val dirfd = args[0].asInt()
        val pathnamePtr: WasmPtr<Byte> = args[1].asWasmAddr()
        val flags = args[2].asInt().toUInt()

        val errNo = try {
            val path = memory.readZeroTerminatedString(pathnamePtr)
            filesystem.unlinkAt(DirFd(dirfd), path, flags)
            Errno.SUCCESS
        } catch (e: SysException) {
            e.errNo
        }

        return Value.i32(-errNo.code.toLong())
    }
}
