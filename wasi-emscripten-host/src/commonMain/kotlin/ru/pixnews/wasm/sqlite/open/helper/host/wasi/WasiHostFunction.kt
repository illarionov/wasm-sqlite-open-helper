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
import ru.pixnews.wasm.sqlite.open.helper.host.pointer
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.IovecArray
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Size
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.WasiValueTypes.U8

/**
 * WASI Preview1 function descriptors
 *
 * https://github.com/WebAssembly/WASI/blob/main/legacy/preview1/witx/wasi_snapshot_preview1.witx
 */
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

    /**
     * Read environment variable data.
     * The sizes of the buffers should match that returned by `environ_sizes_get`.
     * Key/value pairs are expected to be joined with `=`s, and terminated with `\0`s.
     *
     * (@interface func (export "environ_get")
     *     (param $environ (@witx pointer (@witx pointer u8)))
     *     (param $environ_buf (@witx pointer u8))
     *     (result $error (expected (error $errno)))
     *   )
     */
    ENVIRON_GET(
        wasmName = "environ_get",
        paramTypes = listOf(
            U8.pointer.pointer,
            U8.pointer,
        ),
        retType = I32,
    ),

    /**
     * Return environment variable data sizes.
     *
     * (@interface func (export "environ_sizes_get")
     *   ;;; Returns the number of environment variable arguments and the size of the
     *   ;;; environment variable data.
     *   (result $error (expected (tuple $size $size) (error $errno)))
     * )
     *
     */
    ENVIRON_SIZES_GET(
        wasmName = "environ_sizes_get",
        paramTypes = listOf(
            Size.pointer, // *environ_count
            Size.pointer, // *environ_buf_size
        ),
        retType = Errno.wasmValueType,
    ),
    FD_CLOSE(
        wasmName = "fd_close",
        paramTypes = listOf(
            Fd.wasmValueType, // Fd
        ),
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
        paramTypes = listOf(
            Fd.wasmValueType, // Fd
            IovecArray.pointer, // iov
            I32, // iov_cnt
            I32.pointer, // pNum
        ),
        retType = Errno.wasmValueType,
    ),
    FD_PREAD(
        wasmName = "fd_pread",
        paramTypes = listOf(
            Fd.wasmValueType, // Fd
            IovecArray.pointer, // iov
            I32, // iov_cnt
            I32.pointer, // pNum
        ),
        retType = Errno.wasmValueType,
    ),
    FD_SEEK(
        wasmName = "fd_seek",
        paramTypes = listOf(
            Fd.wasmValueType, // fd
            I64, // offset
            I32, // whence
            I64.pointer, // *newOffset
        ),
        retType = Errno.wasmValueType,
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
