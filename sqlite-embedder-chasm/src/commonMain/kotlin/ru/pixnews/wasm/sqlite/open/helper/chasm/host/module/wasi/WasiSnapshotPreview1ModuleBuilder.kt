/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi

import io.github.charlietap.chasm.embedding.function
import io.github.charlietap.chasm.executor.runtime.instance.HostFunction
import io.github.charlietap.chasm.executor.runtime.store.Store
import io.github.charlietap.chasm.executor.runtime.value.NumberValue.I32
import io.github.charlietap.chasm.import.Import
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.toChasmFunctionTypes
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.memory.ChasmMemoryAdapter
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi.function.EnvironGet
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi.function.EnvironSizesGet
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi.function.FdClose
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi.function.FdReadFdPread.Companion.fdPread
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi.function.FdReadFdPread.Companion.fdRead
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi.function.FdSeek
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi.function.FdSync
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi.function.FdWriteFdPwrite.Companion.fdPwrite
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi.function.FdWriteFdPwrite.Companion.fdWrite
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.wasi.function.NotImplementedWasiFunction
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmModules.WASI_SNAPSHOT_PREVIEW1_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
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

internal fun getWasiPreview1HostFunctions(
    store: Store,
    memory: ChasmMemoryAdapter,
    host: EmbedderHost,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1_MODULE_NAME,
): List<Import> {
    val functionTypes = WasiHostFunction.entries.map(WasiHostFunction::type).toChasmFunctionTypes()
    return WasiHostFunction.entries.map { wasiFunc ->
        Import(
            moduleName = moduleName,
            entityName = wasiFunc.wasmName,
            value = function(
                store = store,
                type = functionTypes.getValue(wasiFunc.type),
                function = wasiFunc.createWasiHostFunctionHandle(host, memory).toChasmHostFunction(),
            ),
        )
    }
}

private fun WasiHostFunctionHandle.toChasmHostFunction(): HostFunction = { args ->
    listOf(I32(this.invoke(args).code))
}

private fun WasiHostFunction.createWasiHostFunctionHandle(
    host: EmbedderHost,
    memory: Memory,
): WasiHostFunctionHandle = when (this) {
    ENVIRON_GET -> EnvironGet(host, memory)
    ENVIRON_SIZES_GET -> EnvironSizesGet(host, memory)
    FD_CLOSE -> FdClose(host)
    FD_PREAD -> fdPread(host, memory)
    FD_PWRITE -> fdPwrite(host, memory)
    FD_READ -> fdRead(host, memory)
    FD_SEEK -> FdSeek(host, memory)
    FD_SYNC -> FdSync(host)
    FD_WRITE -> fdWrite(host, memory)
    else -> NotImplementedWasiFunction(this)
}
