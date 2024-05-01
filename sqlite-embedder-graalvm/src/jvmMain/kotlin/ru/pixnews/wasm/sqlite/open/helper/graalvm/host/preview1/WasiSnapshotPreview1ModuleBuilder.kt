/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.preview1

import org.graalvm.polyglot.Context
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmModule
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.setupImportedEnvMemory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.setupWasmModuleFunctions
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.withWasmContext
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.NodeFactory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.notImplementedFunctionNodeFactory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.preview1.func.EnvironGet
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.preview1.func.EnvironSizesGet
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.preview1.func.FdClose
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.preview1.func.FdSeek
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.preview1.func.FdSync
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.preview1.func.SchedYield
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.preview1.func.fdPread
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.preview1.func.fdPwrite
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.preview1.func.fdRead
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.preview1.func.fdWrite
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction

internal class WasiSnapshotPreview1ModuleBuilder(
    private val graalContext: Context,
    private val host: SqliteEmbedderHost,
    private val moduleName: String = WASI_SNAPSHOT_PREVIEW1,
) {
    private val preview1Functions: Map<out HostFunction, NodeFactory> = mapOf(
        WasiHostFunction.ARGS_GET to notImplementedFunctionNodeFactory,
        WasiHostFunction.ARGS_SIZES_GET to notImplementedFunctionNodeFactory,
        WasiHostFunction.CLOCK_TIME_GET to notImplementedFunctionNodeFactory,
        WasiHostFunction.ENVIRON_GET to ::EnvironGet,
        WasiHostFunction.ENVIRON_SIZES_GET to ::EnvironSizesGet,
        WasiHostFunction.FD_CLOSE to ::FdClose,
        WasiHostFunction.FD_FDSTAT_GET to notImplementedFunctionNodeFactory,
        WasiHostFunction.FD_FDSTAT_SET_FLAGS to notImplementedFunctionNodeFactory,
        WasiHostFunction.FD_FILESTAT_GET to notImplementedFunctionNodeFactory,
        WasiHostFunction.FD_PRESTAT_DIR_NAME to notImplementedFunctionNodeFactory,
        WasiHostFunction.FD_PRESTAT_GET to notImplementedFunctionNodeFactory,
        WasiHostFunction.FD_READ to ::fdRead,
        WasiHostFunction.FD_PREAD to ::fdPread,
        WasiHostFunction.FD_SEEK to ::FdSeek,
        WasiHostFunction.FD_SYNC to ::FdSync,
        WasiHostFunction.FD_WRITE to ::fdWrite,
        WasiHostFunction.FD_PWRITE to ::fdPwrite,
        WasiHostFunction.PATH_CREATE_DIRECTORY to notImplementedFunctionNodeFactory,
        WasiHostFunction.PATH_FILESTAT_GET to notImplementedFunctionNodeFactory,
        WasiHostFunction.PATH_FILESTAT_SET_TIMES to notImplementedFunctionNodeFactory,
        WasiHostFunction.PATH_LINK to notImplementedFunctionNodeFactory,
        WasiHostFunction.PATH_OPEN to notImplementedFunctionNodeFactory,
        WasiHostFunction.PATH_READLINK to notImplementedFunctionNodeFactory,
        WasiHostFunction.PATH_REMOVE_DIRECTORY to notImplementedFunctionNodeFactory,
        WasiHostFunction.PATH_RENAME to notImplementedFunctionNodeFactory,
        WasiHostFunction.PATH_SYMLINK to notImplementedFunctionNodeFactory,
        WasiHostFunction.PATH_UNLINK_FILE to notImplementedFunctionNodeFactory,
        WasiHostFunction.RANDOM_GET to notImplementedFunctionNodeFactory,
        WasiHostFunction.SCHED_YIELD to ::SchedYield,
    ).also {
        check(it.size == WasiHostFunction.entries.size)
    }

    fun setupModule(
        sharedMemory: Boolean = false,
        useUnsafeMemory: Boolean = false,
    ): WasmInstance = graalContext.withWasmContext { wasmContext ->
        val wasiModule = WasmModule.create(moduleName, null)
        wasiModule.setupImportedEnvMemory(
            wasmContext,
            sharedMemory = sharedMemory,
            useUnsafeMemory = useUnsafeMemory,
        )
        return setupWasmModuleFunctions(wasmContext, host, wasiModule, preview1Functions)
    }

    companion object {
        internal const val WASI_SNAPSHOT_PREVIEW1 = "wasi_snapshot_preview1"
    }
}
