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
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.memory.ChicoryMemoryAdapter
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.emscriptenEnvHostFunction
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readZeroTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.pack
import ru.pixnews.wasm.sqlite.open.helper.host.pointer
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.WasiValueTypes.U8
import java.util.logging.Logger

internal fun syscallLstat64(
    memory: ChicoryMemoryAdapter,
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = stat64Func(
    memory = memory,
    filesystem = filesystem,
    fieldName = "__syscall_lstat64",
    followSymlinks = false,
    moduleName = moduleName,
)

internal fun syscallStat64(
    memory: ChicoryMemoryAdapter,
    filesystem: FileSystem,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = stat64Func(
    memory = memory,
    filesystem = filesystem,
    fieldName = "__syscall_stat64",
    followSymlinks = true,
    moduleName = moduleName,
)

private fun stat64Func(
    memory: ChicoryMemoryAdapter,
    filesystem: FileSystem,
    fieldName: String,
    followSymlinks: Boolean = true,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = fieldName,
    paramTypes = listOf(
        U8.pointer, // pathname
        U8.pointer, // statbuf
    ),
    returnType = Errno.wasmValueType,
    moduleName = moduleName,
    handle = Stat64(memory = memory, filesystem = filesystem, followSymlinks = followSymlinks),
)

private class Stat64(
    private val memory: ChicoryMemoryAdapter,
    private val filesystem: FileSystem,
    private val followSymlinks: Boolean = false,
    private val logger: Logger = Logger.getLogger(Stat64::class.qualifiedName),
) : EmscriptenHostFunction {
    private val syscallName = if (followSymlinks) "Stat64" else "Lstat64"

    override fun apply(instance: Instance, vararg args: Value): Value {
        val result = stat64(
            instance,
            args[0].asWasmAddr(),
            args[1].asWasmAddr(),
        )
        return Value.i32(result.toLong())
    }

    private fun stat64(
        instance: Instance,
        pathnamePtr: WasmPtr<Byte>,
        dst: WasmPtr<StructStat>,
    ): Int {
        var path = ""
        try {
            path = memory.readZeroTerminatedString(pathnamePtr)
            val stat = filesystem.stat(
                path = path,
                followSymlinks = followSymlinks,
            ).also {
                logger.finest { "$syscallName($path): $it" }
            }.pack()
            instance.memory().write(dst.addr, stat)
        } catch (e: SysException) {
            logger.finest { "$syscallName(`$path`): error ${e.errNo}" }
            return -e.errNo.code
        }

        return 0
    }
}
