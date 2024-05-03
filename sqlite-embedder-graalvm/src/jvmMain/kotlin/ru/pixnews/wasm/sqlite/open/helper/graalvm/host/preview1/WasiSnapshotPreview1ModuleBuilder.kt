/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.preview1

import org.graalvm.polyglot.Context
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmModule
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
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction

internal class WasiSnapshotPreview1ModuleBuilder(
    private val graalContext: Context,
    private val host: SqliteEmbedderHost,
    private val moduleName: String = WASI_SNAPSHOT_PREVIEW1,
) {
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
        return setupWasmModuleFunctions(
            wasmContext,
            host,
            wasiModule,
            WasiHostFunction.entries.associateWith { it.nodeFactory },
        )
    }

    companion object {
        internal const val WASI_SNAPSHOT_PREVIEW1 = "wasi_snapshot_preview1"
        private val WasiHostFunction.nodeFactory: NodeFactory
            get() = when (this) {
                WasiHostFunction.ARGS_GET -> notImplementedFunctionNodeFactory
                WasiHostFunction.ARGS_SIZES_GET -> notImplementedFunctionNodeFactory
                WasiHostFunction.CLOCK_RES_GET -> notImplementedFunctionNodeFactory
                WasiHostFunction.CLOCK_TIME_GET -> notImplementedFunctionNodeFactory
                WasiHostFunction.ENVIRON_GET -> ::EnvironGet
                WasiHostFunction.ENVIRON_SIZES_GET -> ::EnvironSizesGet
                WasiHostFunction.FD_ADVISE -> notImplementedFunctionNodeFactory
                WasiHostFunction.FD_ALLOCATE -> notImplementedFunctionNodeFactory
                WasiHostFunction.FD_CLOSE -> ::FdClose
                WasiHostFunction.FD_DATASYNC -> notImplementedFunctionNodeFactory
                WasiHostFunction.FD_FDSTAT_GET -> notImplementedFunctionNodeFactory
                WasiHostFunction.FD_FDSTAT_SET_FLAGS -> notImplementedFunctionNodeFactory
                WasiHostFunction.FD_FDSTAT_SET_RIGHTS -> notImplementedFunctionNodeFactory
                WasiHostFunction.FD_FILESTAT_GET -> notImplementedFunctionNodeFactory
                WasiHostFunction.FD_FILESTAT_SET_SIZE -> notImplementedFunctionNodeFactory
                WasiHostFunction.FD_FILESTAT_SET_TIMES -> notImplementedFunctionNodeFactory
                WasiHostFunction.FD_PREAD -> ::fdPread
                WasiHostFunction.FD_PRESTAT_DIR_NAME -> notImplementedFunctionNodeFactory
                WasiHostFunction.FD_PRESTAT_GET -> notImplementedFunctionNodeFactory
                WasiHostFunction.FD_PWRITE -> ::fdPwrite
                WasiHostFunction.FD_READ -> ::fdRead
                WasiHostFunction.FD_READDIR -> notImplementedFunctionNodeFactory
                WasiHostFunction.FD_RENUMBER -> notImplementedFunctionNodeFactory
                WasiHostFunction.FD_SEEK -> ::FdSeek
                WasiHostFunction.FD_SYNC -> ::FdSync
                WasiHostFunction.FD_TELL -> notImplementedFunctionNodeFactory
                WasiHostFunction.FD_WRITE -> ::fdWrite
                WasiHostFunction.PATH_CREATE_DIRECTORY -> notImplementedFunctionNodeFactory
                WasiHostFunction.PATH_FILESTAT_GET -> notImplementedFunctionNodeFactory
                WasiHostFunction.PATH_FILESTAT_SET_TIMES -> notImplementedFunctionNodeFactory
                WasiHostFunction.PATH_LINK -> notImplementedFunctionNodeFactory
                WasiHostFunction.PATH_OPEN -> notImplementedFunctionNodeFactory
                WasiHostFunction.PATH_READLINK -> notImplementedFunctionNodeFactory
                WasiHostFunction.PATH_REMOVE_DIRECTORY -> notImplementedFunctionNodeFactory
                WasiHostFunction.PATH_RENAME -> notImplementedFunctionNodeFactory
                WasiHostFunction.PATH_SYMLINK -> notImplementedFunctionNodeFactory
                WasiHostFunction.PATH_UNLINK_FILE -> notImplementedFunctionNodeFactory
                WasiHostFunction.POLL_ONEOFF -> notImplementedFunctionNodeFactory
                WasiHostFunction.PROC_EXIT -> notImplementedFunctionNodeFactory
                WasiHostFunction.PROC_RAISE -> notImplementedFunctionNodeFactory
                WasiHostFunction.RANDOM_GET -> notImplementedFunctionNodeFactory
                WasiHostFunction.SCHED_YIELD -> ::SchedYield
                WasiHostFunction.SOCK_ACCEPT -> notImplementedFunctionNodeFactory
                WasiHostFunction.SOCK_RECV -> notImplementedFunctionNodeFactory
                WasiHostFunction.SOCK_SEND -> notImplementedFunctionNodeFactory
                WasiHostFunction.SOCK_SHUTDOWN -> notImplementedFunctionNodeFactory
            }
    }
}
