/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("NoMultipleSpaces")

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.chicory
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.function.EnvironGet
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.function.EnvironSizesGet
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.function.FdClose
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.function.FdReadFdPread.Companion.fdPread
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.function.FdReadFdPread.Companion.fdRead
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.function.FdSeek
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.function.FdSync
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.function.FdWriteFdPwrite.Companion.fdPwrite
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.function.FdWriteFdPwrite.Companion.fdWrite
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.function.SchedYield
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.function.notImplementedWasiHostFunctionHandleFactory
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmValueType
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction
import com.dylibso.chicory.runtime.WasmFunctionHandle as ChicoryWasmFunctionHandle

// https://github.com/WebAssembly/WASI/tree/main
internal class WasiSnapshotPreview1Builtins(
    private val memory: Memory,
    private val host: SqliteEmbedderHost,
) {
    fun asChicoryHostFunctions(
        moduleName: String = WASI_SNAPSHOT_PREVIEW1_MODULE_NAME,
    ): List<HostFunction> {
        return WasiHostFunction.entries.map { wasiFunc ->
            HostFunction(
                WasiHostFunctionAdapter(wasiFunc.functionFactory(host, memory)),
                moduleName,
                wasiFunc.wasmName,
                wasiFunc.type.paramTypes.map(WasmValueType::chicory),
                wasiFunc.type.returnTypes.map(WasmValueType::chicory),
            )
        }
    }

    private class WasiHostFunctionAdapter(
        private val delegate: WasiHostFunctionHandle,
    ) : ChicoryWasmFunctionHandle {
        override fun apply(instance: Instance, vararg args: Value): Array<Value> {
            val result = delegate.apply(instance, args = args)
            return arrayOf(Value.i32(result.code.toLong()))
        }
    }

    private companion object {
        const val WASI_SNAPSHOT_PREVIEW1_MODULE_NAME = "wasi_snapshot_preview1"

        val WasiHostFunction.functionFactory: WasiHostFunctionHandleFactory
            get() = when (this) {
                WasiHostFunction.ARGS_GET -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.ARGS_SIZES_GET -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.CLOCK_RES_GET -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.CLOCK_TIME_GET -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.ENVIRON_GET -> ::EnvironGet
                WasiHostFunction.ENVIRON_SIZES_GET -> ::EnvironSizesGet
                WasiHostFunction.FD_ADVISE -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.FD_ALLOCATE -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.FD_CLOSE -> ::FdClose
                WasiHostFunction.FD_DATASYNC -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.FD_FDSTAT_GET -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.FD_FDSTAT_SET_FLAGS -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.FD_FDSTAT_SET_RIGHTS -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.FD_FILESTAT_GET -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.FD_FILESTAT_SET_SIZE -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.FD_FILESTAT_SET_TIMES -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.FD_PREAD -> ::fdPread
                WasiHostFunction.FD_PRESTAT_DIR_NAME -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.FD_PRESTAT_GET -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.FD_PWRITE -> ::fdPwrite
                WasiHostFunction.FD_READ -> ::fdRead
                WasiHostFunction.FD_READDIR -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.FD_RENUMBER -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.FD_SEEK -> ::FdSeek
                WasiHostFunction.FD_SYNC -> ::FdSync
                WasiHostFunction.FD_TELL -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.FD_WRITE -> ::fdWrite
                WasiHostFunction.PATH_CREATE_DIRECTORY -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.PATH_FILESTAT_GET -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.PATH_FILESTAT_SET_TIMES -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.PATH_LINK -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.PATH_OPEN -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.PATH_READLINK -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.PATH_REMOVE_DIRECTORY -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.PATH_RENAME -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.PATH_SYMLINK -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.PATH_UNLINK_FILE -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.POLL_ONEOFF -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.PROC_EXIT -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.PROC_RAISE -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.RANDOM_GET -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.SCHED_YIELD -> ::SchedYield
                WasiHostFunction.SOCK_ACCEPT -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.SOCK_RECV -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.SOCK_SEND -> notImplementedWasiHostFunctionHandleFactory
                WasiHostFunction.SOCK_SHUTDOWN -> notImplementedWasiHostFunctionHandleFactory
            }
    }
}
