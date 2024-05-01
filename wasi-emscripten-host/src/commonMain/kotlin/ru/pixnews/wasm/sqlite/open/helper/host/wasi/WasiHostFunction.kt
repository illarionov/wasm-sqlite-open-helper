/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MagicNumber")

package ru.pixnews.wasm.sqlite.open.helper.host.wasi

import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.I64
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunction.HostFunctionType

public enum class WasiHostFunction(
    public override val wasmName: String,
    public override val type: HostFunctionType,
) : HostFunction {
    ARGS_GET(
        wasmName = "args_get",
        paramTypes = listOf(I32, I32),
        retType = I32,
    ),
    ARGS_SIZES_GET(
        wasmName = "args_sizes_get",
        paramTypes = listOf(I32, I32),
        retType = I32,
    ),
    CLOCK_TIME_GET(
        wasmName = "clock_time_get",
        paramTypes = listOf(I32, I64, I32),
        retType = I32,
    ),
    ENVIRON_GET(
        wasmName = "environ_get",
        paramTypes = listOf(I32, I32),
        retType = I32,
    ),
    ENVIRON_SIZES_GET(
        wasmName = "environ_sizes_get",
        paramTypes = listOf(I32, I32),
        retType = I32,
    ),
    FD_CLOSE(
        wasmName = "fd_close",
        paramTypes = listOf(I32),
        retType = I32,
    ),
    FD_FDSTAT_GET(
        wasmName = "fd_fdstat_get",
        paramTypes = listOf(I32, I32),
        retType = I32,
    ),
    FD_FDSTAT_SET_FLAGS(
        wasmName = "fd_fdstat_set_flags",
        paramTypes = listOf(I32, I32),
        retType = I32,
    ),
    FD_FILESTAT_GET(
        wasmName = "fd_filestat_get",
        paramTypes = listOf(I32, I32),
        retType = I32,
    ),
    FD_PRESTAT_DIR_NAME(
        wasmName = "fd_prestat_dir_name",
        paramTypes = listOf(I32, I32, I32),
        retType = I32,
    ),
    FD_PRESTAT_GET(
        wasmName = "fd_prestat_get",
        paramTypes = listOf(I32, I32),
        retType = I32,
    ),
    FD_READ(
        wasmName = "fd_read",
        paramTypes = List(4) { I32 },
        retType = I32,
    ),
    FD_PREAD(
        wasmName = "fd_pread",
        paramTypes = List(4) { I32 },
        retType = I32,
    ),
    FD_SEEK(
        wasmName = "fd_seek",
        paramTypes = listOf(I32, I64, I32, I32),
        retType = I32,
    ),
    FD_SYNC(
        wasmName = "fd_sync",
        paramTypes = listOf(I32),
        retType = I32,
    ),
    FD_WRITE(
        wasmName = "fd_write",
        paramTypes = List(4) { I32 },
        retType = I32,
    ),
    FD_PWRITE(
        wasmName = "fd_pwrite",
        paramTypes = List(4) { I32 },
        retType = I32,
    ),
    PATH_CREATE_DIRECTORY(
        wasmName = "path_create_directory",
        paramTypes = List(3) { I32 },
        retType = I32,
    ),
    PATH_FILESTAT_GET(
        wasmName = "path_filestat_get",
        paramTypes = List(5) { I32 },
        retType = I32,
    ),
    PATH_FILESTAT_SET_TIMES(
        wasmName = "path_filestat_set_times",
        paramTypes = listOf(I32, I32, I32, I32, I64, I64, I32),
        retType = I32,
    ),
    PATH_LINK(
        wasmName = "path_link",
        paramTypes = List(7) { I32 },
        retType = I32,
    ),
    PATH_OPEN(
        wasmName = "path_open",
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
        retType = I32,
    ),
    PATH_READLINK(
        wasmName = "path_readlink",
        paramTypes = List(5) { I32 },
        retType = I32,
    ),
    PATH_REMOVE_DIRECTORY(
        wasmName = "path_remove_directory",
        paramTypes = listOf(I32, I32, I32),
        retType = I32,
    ),
    PATH_RENAME(
        wasmName = "path_rename",
        paramTypes = List(6) { I32 },
        retType = I32,
    ),
    PATH_SYMLINK(
        wasmName = "path_symlink",
        paramTypes = List(5) { I32 },
        retType = I32,
    ),
    PATH_UNLINK_FILE(
        wasmName = "path_unlink_file",
        paramTypes = listOf(I32, I32, I32),
        retType = I32,
    ),
    RANDOM_GET(
        wasmName = "random_get",
        paramTypes = listOf(I32, I32),
        retType = I32,
    ),
    SCHED_YIELD(
        wasmName = "sched_yield",
        paramTypes = listOf(),
        retType = I32,
    ),
    ;

    constructor(
        wasmName: String,
        paramTypes: List<WasmValueType>,
        retType: WasmValueType? = null,
    ) : this(
        wasmName = wasmName,
        type = HostFunctionType(
            params = paramTypes,
            returnTypes = if (retType != null) {
                listOf(retType)
            } else {
                emptyList()
            },
        ),
    )

    public companion object {
        public val byWasmName: Map<String, HostFunction> = entries.associateBy(WasiHostFunction::wasmName)

        init {
            check(entries.size == byWasmName.size)
        }
    }
}
