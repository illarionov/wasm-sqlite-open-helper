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
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.function.NotImplemented
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.function.SchedYield
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmModules.WASI_SNAPSHOT_PREVIEW1_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmValueType
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction.ENVIRON_GET
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction.ENVIRON_SIZES_GET
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction.FD_CLOSE
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction.FD_PREAD
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction.FD_PWRITE
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction.FD_READ
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction.FD_SEEK
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction.FD_SYNC
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction.FD_WRITE
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction.SCHED_YIELD
import com.dylibso.chicory.runtime.WasmFunctionHandle as ChicoryWasmFunctionHandle

// https://github.com/WebAssembly/WASI/tree/main
internal class WasiSnapshotPreview1ModuleBuilder(
    private val memory: Memory,
    private val wasiMemoryReader: WasiMemoryReader,
    private val wasiMemoryWriter: WasiMemoryWriter,
    private val host: EmbedderHost,
) {
    fun asChicoryHostFunctions(
        moduleName: String = WASI_SNAPSHOT_PREVIEW1_MODULE_NAME,
    ): List<HostFunction> {
        return WasiHostFunction.entries.map { wasiFunc ->
            val functionHandle = wasiFunc.createWasiHostFunctionHandle(host, memory, wasiMemoryReader, wasiMemoryWriter)
            HostFunction(
                WasiHostFunctionAdapter(functionHandle),
                moduleName,
                wasiFunc.wasmName,
                wasiFunc.type.paramTypes.map(WasmValueType::chicory),
                wasiFunc.type.returnTypes.map(WasmValueType::chicory),
            )
        }
    }

    private fun WasiHostFunction.createWasiHostFunctionHandle(
        host: EmbedderHost,
        memory: Memory,
        wasiMemoryReader: WasiMemoryReader,
        wasiMemoryWriter: WasiMemoryWriter,
    ): WasiHostFunctionHandle = when (this) {
        ENVIRON_GET -> EnvironGet(host, memory)
        ENVIRON_SIZES_GET -> EnvironSizesGet(host, memory)
        FD_CLOSE -> FdClose(host)
        FD_PREAD -> fdPread(host, memory, wasiMemoryReader)
        FD_PWRITE -> fdPwrite(host, memory, wasiMemoryWriter)
        FD_READ -> fdRead(host, memory, wasiMemoryReader)
        FD_SEEK -> FdSeek(host, memory)
        FD_SYNC -> FdSync(host)
        FD_WRITE -> fdWrite(host, memory, wasiMemoryWriter)
        SCHED_YIELD -> SchedYield(host)
        else -> NotImplemented
    }

    private class WasiHostFunctionAdapter(
        private val delegate: WasiHostFunctionHandle,
    ) : ChicoryWasmFunctionHandle {
        override fun apply(instance: Instance, vararg args: Value): Array<Value> {
            val result = delegate.apply(instance, args = args)
            return arrayOf(Value.i32(result.code.toLong()))
        }
    }
}
