/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func

import com.dylibso.chicory.runtime.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.wasiHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.pointer
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.ext.WasiEnvironmentFunc
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Size

/**
 * Return environment variable data sizes.
 *
 * https://github.com/WebAssembly/WASI/blob/main/legacy/preview1/witx/wasi_snapshot_preview1.witx
 *
 * (@interface func (export "environ_sizes_get")
 *   ;;; Returns the number of environment variable arguments and the size of the
 *   ;;; environment variable data.
 *   (result $error (expected (tuple $size $size) (error $errno)))
 * )
 *
 */
@Suppress("LAMBDA_IS_NOT_LAST_PARAMETER")
internal fun environSizesGet(
    memory: Memory,
    envProvider: () -> Map<String, String> = System::getenv,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = wasiHostFunction(
    funcName = "environ_sizes_get",
    paramTypes = listOf(
        Size.pointer, // *environ_count
        Size.pointer, // *environ_buf_size
    ),
    moduleName = moduleName,
) { _, params ->
    WasiEnvironmentFunc.environSizesGet(
        memory,
        params[0].asWasmAddr(),
        params[1].asWasmAddr(),
        envProvider,
    )
}
