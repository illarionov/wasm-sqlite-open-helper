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
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.fn
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
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.I64

internal class WasiSnapshotPreview1MobuleBuilder(
    private val graalContext: Context,
    private val host: SqliteEmbedderHost,
    private val moduleName: String = WASI_SNAPSHOT_PREVIEW1,
) {
    private val preview1Functions: List<HostFunction> = buildList {
        fn(
            name = "args_get",
            paramTypes = listOf(I32, I32),
            // nodeFactory =
        )
        fn(
            name = "args_sizes_get",
            paramTypes = listOf(I32, I32),
            // nodeFactory =
        )
        fn(
            name = "clock_time_get",
            paramTypes = listOf(I32, I64, I32),
            // nodeFactory =
        )
        fn(
            name = "environ_get",
            paramTypes = listOf(I32, I32),
            nodeFactory = ::EnvironGet,
        )
        fn(
            name = "environ_sizes_get",
            paramTypes = listOf(I32, I32),
            nodeFactory = ::EnvironSizesGet,
        )
        fn(
            name = "fd_close",
            paramTypes = listOf(I32),
            nodeFactory = ::FdClose,
        )
        fn(
            name = "fd_fdstat_get",
            paramTypes = listOf(I32, I32),
            // nodeFactory =
        )
        fn(
            name = "fd_fdstat_set_flags",
            paramTypes = listOf(I32, I32),
            // nodeFactory =
        )
        fn(
            name = "fd_filestat_get",
            paramTypes = listOf(I32, I32),
            // nodeFactory =
        )
        fn(
            name = "fd_prestat_dir_name",
            paramTypes = listOf(I32, I32, I32),
            // nodeFactory =
        )
        fn(
            name = "fd_prestat_get",
            paramTypes = listOf(I32, I32),
            // nodeFactory =
        )
        fn(
            name = "fd_read",
            paramTypes = List(4) { I32 },
            nodeFactory = ::fdRead,
        )
        fn(
            name = "fd_pread",
            paramTypes = List(4) { I32 },
            nodeFactory = ::fdPread,
        )
        fn(
            name = "fd_seek",
            paramTypes = listOf(I32, I64, I32, I32),
            nodeFactory = ::FdSeek,
        )
        fn(
            name = "fd_sync",
            paramTypes = listOf(I32),
            nodeFactory = ::FdSync,
        )
        fn(
            name = "fd_write",
            paramTypes = List(4) { I32 },
            nodeFactory = ::fdWrite,
        )
        fn(
            name = "fd_pwrite",
            paramTypes = List(4) { I32 },
            nodeFactory = ::fdPwrite,
        )
        fn(
            name = "path_create_directory",
            paramTypes = List(3) { I32 },
            // nodeFactory =
        )
        fn(
            name = "path_filestat_get",
            paramTypes = List(5) { I32 },
            // nodeFactory =
        )
        fn(
            name = "path_filestat_set_times",
            paramTypes = listOf(I32, I32, I32, I32, I64, I64, I32),
            // nodeFactory =
        )
        fn(
            name = "path_link",
            paramTypes = List(7) { I32 },
            // nodeFactory =
        )
        fn(
            name = "path_open",
            paramTypes = listOf(
                I32,
                I32,
                I32,
                I32,
                I32,
                I64,
                I64,
                I32,
                I32,
            ),
            // nodeFactory =
        )
        fn(
            name = "path_readlink",
            paramTypes = List(5) { I32 },
            // nodeFactory =
        )
        fn(
            name = "path_remove_directory",
            paramTypes = listOf(I32, I32, I32),
            // nodeFactory =
        )
        fn(
            name = "path_rename",
            paramTypes = List(6) { I32 },
            // nodeFactory =
        )
        fn(
            name = "path_symlink",
            paramTypes = List(5) { I32 },
            // nodeFactory =
        )
        fn(
            name = "path_unlink_file",
            paramTypes = listOf(I32, I32, I32),
            // nodeFactory =
        )
        fn(
            name = "random_get",
            paramTypes = listOf(I32, I32),
            // nodeFactory =
        )
        fn(
            name = "sched_yield",
            paramTypes = listOf(),
            nodeFactory = ::SchedYield,
        )
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
